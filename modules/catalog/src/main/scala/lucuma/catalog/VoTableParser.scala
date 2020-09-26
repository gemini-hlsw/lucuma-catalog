// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.data.Validated._
import cats.data._
import cats.implicits._
import coulomb._
import eu.timepit.refined._
import eu.timepit.refined.cats.syntax._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import fs2._
import fs2.data.xml._
import fs2.data.xml.XmlEvent._
import lucuma.catalog._
import lucuma.catalog.CatalogProblem._
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.RightAscension
import lucuma.core.model.Target
import monocle.function.Index.listIndex
import monocle.macros.Lenses
import scala.collection.immutable.SortedMap
import lucuma.core.math.Parallax
import lucuma.core.math.RadialVelocity
import lucuma.core.math.Redshift
import lucuma.core.math.units.KilometersPerSecond

@Lenses
private[catalog] case class PartialTableRowItem(field: FieldDescriptor)

@Lenses
private[catalog] case class PartialTableRow(
  items: List[Either[PartialTableRowItem, TableRowItem]]
) {
  def isComplete: Boolean  = items.forall(_.isRight)
  def toTableRow: TableRow = TableRow(items.collect { case Right(r) => r }.reverse)
}

object VoTableParser extends VoTableParser {

  val UCD_OBJID      = Ucd.unsafeFromString("meta.id;meta.main")
  val UCD_TYPEDID    = Ucd.unsafeFromString("meta.id")
  val UCD_EPOCH      = Ucd.unsafeFromString("meta.ref;time.epoch")
  val UCD_RA         = Ucd.unsafeFromString("pos.eq.ra;meta.main")
  val UCD_DEC        = Ucd.unsafeFromString("pos.eq.dec;meta.main")
  val UCD_PMDEC      = Ucd.unsafeFromString("pos.pm;pos.eq.dec")
  val UCD_PMRA       = Ucd.unsafeFromString("pos.pm;pos.eq.ra")
  val UCD_RV         = Ucd.unsafeFromString("spect.dopplerVeloc.opt")
  val UCD_Z          = Ucd.unsafeFromString("src.redshift")
  val UCD_PLX        = Ucd.unsafeFromString("pos.parallax.trig")
  val UCD_PHOTO_FLUX = Ucd.unsafeFromString("phot.flux")

  val UCD_MAG  = refineMV[NonEmpty]("phot.mag")
  val STAT_ERR = refineMV[NonEmpty]("stat.error")
}

trait VoTableParser {

  protected def fd[F[_]]: Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, FieldDescriptor]] = {
    def go(
      s:  Stream[F, XmlEvent],
      fd: ValidatedNec[CatalogProblem, FieldDescriptor]
    ): Pull[F, ValidatedNec[CatalogProblem, FieldDescriptor], Unit] =
      s.pull.uncons1.flatMap {
        case Some((StartTag(QName(_, "FIELD"), xmlAttr, _), s)) =>
          val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
          val name = attr.get("name")

          val id: ValidatedNec[CatalogProblem, NonEmptyString] =
            NonEmptyString
              .validateNec(attr.get("ID").orElse(name).orEmpty)
              .leftMap(_ => NonEmptyChain.one(MissingXmlAttribute("ID")))

          val ucd: ValidatedNec[CatalogProblem, Ucd] = Validated
            .fromOption(attr.get("ucd"), MissingXmlAttribute("ucd"))
            .toValidatedNec
            .andThen(Ucd.apply)

          val nameV = Validated.fromOption(name, MissingXmlAttribute("name")).toValidatedNec
          go(s,
             ((id, ucd).mapN(FieldId.apply), nameV).mapN { (f, n) =>
               FieldDescriptor(f, n)
             }
          )
        case Some((StartTag(QName(_, t), _, _), _))             =>
          Pull.output1(Validated.invalidNec(UnknownXmlTag(t))) >> Pull.done
        case Some((EndTag(QName(_, "FIELD")), _))               =>
          Pull.output1(fd)
        case Some((_, _))                                       =>
          Pull.output1(fd) >> Pull.done
        case None                                               => Pull.done
      }
    in =>
      go(in, Validated.invalidNec[CatalogProblem, FieldDescriptor](MissingXmlTag("FIELD"))).stream
  }

  protected def fds[F[_]]
    : Pipe[F, XmlEvent, List[ValidatedNec[CatalogProblem, FieldDescriptor]]] = {
    def go(
      s: Stream[F, XmlEvent],
      l: List[ValidatedNec[CatalogProblem, FieldDescriptor]]
    ): Pull[F, List[ValidatedNec[CatalogProblem, FieldDescriptor]], Unit] =
      s.pull.uncons1.flatMap {
        case Some((StartTag(QName(_, "TABLE"), _, _), s))       =>
          go(s, l)
        case Some((StartTag(QName(_, "FIELD"), xmlAttr, _), s)) =>
          val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
          val name = attr.get("name")

          val id: ValidatedNec[CatalogProblem, NonEmptyString] =
            NonEmptyString
              .validateNec(attr.get("ID").orElse(name).orEmpty)
              .leftMap(_ => NonEmptyChain.one(MissingXmlAttribute("ID")))

          val ucd: ValidatedNec[CatalogProblem, Ucd] = Validated
            .fromOption(attr.get("ucd"), MissingXmlAttribute("ucd"))
            .toValidatedNec
            .andThen(Ucd.apply)

          val nameV = Validated.fromOption(name, MissingXmlAttribute("name")).toValidatedNec
          go(s,
             ((id, ucd).mapN(FieldId.apply), nameV).mapN { (f, n) =>
               FieldDescriptor(f, n)
             } :: l
          )
        case Some((EndTag(QName(_, "TABLE")), _))               =>
          Pull.output1(l.reverse) >> Pull.done
        case Some((StartTag(QName(_, t), _, _), _))             =>
          Pull.output1(List(Validated.invalidNec(UnknownXmlTag(t)))) >> Pull.done
        case Some((EndTag(QName(_, "FIELD")), s))               =>
          go(s, l)
        case Some((_, s))                                       =>
          go(s, l)
        case None                                               => Pull.done
      }

    in => go(in, Nil).stream
  }

  protected def tr[F[_]](
    fields: List[FieldDescriptor]
  ): Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, TableRow]] = {
    def go(
      s: Stream[F, XmlEvent],
      t: PartialTableRow,
      f: List[FieldDescriptor]
    ): Pull[F, ValidatedNec[CatalogProblem, TableRow], Unit] =
      s.pull.uncons1.flatMap {
        case Some((StartTag(QName(_, "TD"), _, _), s)) =>
          f match {
            case head :: tail =>
              go(s, PartialTableRow.items.modify(PartialTableRowItem(head).asLeft :: _)(t), tail)
            case Nil          =>
              Pull.output1(Validated.invalidNec(ExtraRow)) >> Pull.done
          }
        case Some((EndTag(QName(_, "TR")), _))         =>
          f match {
            case x :: l
                if f =!= fields || t.items.length =!= fields.length => // this indicates we have a mismatch between fields and data
              Pull.output1(
                Validated.invalid(
                  NonEmptyChain(MissingValue(x.id), l.map(x => MissingValue(x.id)): _*)
                )
              ) >> Pull.done
            case Nil
                if t.items.length =!= fields.length => // this indicates we have a mismatch between fields and data
              Pull.output1(
                Validated.invalid(
                  NonEmptyChain
                    .fromSeq(fields.map(x => MissingValue(x.id)))
                    .getOrElse(NonEmptyChain.one(MissingRow))
                )
              ) >> Pull.done
            case _ =>
              Pull.output1(Validated.validNec(t.toTableRow)) >> Pull.done
          }
        case Some((XmlString(v, _), s))                =>
          go(s,
             PartialTableRow.items
               .composeOptional(listIndex.index(0))
               .modify {
                 case ti @ Right(_) => ti
                 case Left(pti)     => TableRowItem(pti.field, v).asRight
               }(t),
             f
          )
        case Some((_, s))                              =>
          go(s, t, f)
        case None                                      => Pull.done
      }
    in => go(in, PartialTableRow(Nil), fields).stream
  }

  protected def trs[F[_]](
    fields: List[FieldDescriptor]
  ): Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, TableRow]] = {
    def go(
      s: Stream[F, XmlEvent],
      t: PartialTableRow,
      f: List[FieldDescriptor]
    ): Pull[F, ValidatedNec[CatalogProblem, TableRow], Unit] =
      s.pull.uncons1.flatMap {
        case Some((StartTag(QName(_, "TD"), _, _), s)) =>
          f match {
            case head :: tail =>
              go(s, PartialTableRow.items.modify(PartialTableRowItem(head).asLeft :: _)(t), tail)
            case Nil          =>
              Pull.output1(Validated.invalidNec(ExtraRow)) >> Pull.done
          }
        case Some((EndTag(QName(_, "TR")), s))         =>
          f match {
            case x :: l =>
              Pull.output1(
                Validated.invalid(
                  NonEmptyChain(MissingValue(x.id), l.map(x => MissingValue(x.id)): _*)
                )
              ) >> go(s, PartialTableRow(Nil), fields)
            case Nil
                if t.items.length =!= fields.length => // this indicates we have a mismatch between fields and data
              Pull.output1(
                Validated.invalid(
                  NonEmptyChain
                    .fromSeq(fields.map(x => MissingValue(x.id)))
                    .getOrElse(NonEmptyChain.one(MissingRow))
                )
              ) >> Pull.done
            case _      =>
              Pull.output1(Validated.validNec(t.toTableRow)) >> go(s, PartialTableRow(Nil), fields)
          }
        case Some((XmlString(v, _), s))                =>
          go(s,
             PartialTableRow.items
               .composeOptional(listIndex.index(0))
               .modify {
                 case ti @ Right(_) => ti
                 case Left(pti)     => TableRowItem(pti.field, v).asRight
               }(t),
             f
          )
        case Some((_, s))                              =>
          go(s, t, f)
        case None                                      => Pull.done
      }
    in => go(in, PartialTableRow(Nil), fields).stream
  }

  protected def trsf[F[_]]: Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, TableRow]] = {
    def go(
      s:      Stream[F, XmlEvent],
      t:      PartialTableRow,
      f:      List[FieldDescriptor],
      fields: Option[NonEmptyList[FieldDescriptor]]
    ): Pull[F, ValidatedNec[CatalogProblem, TableRow], Unit] =
      s.pull.uncons1.flatMap {
        case Some((StartTag(QName(_, "FIELD"), xmlAttr, _), s)) =>
          val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
          val name = attr.get("name")

          val id: ValidatedNec[CatalogProblem, NonEmptyString] =
            NonEmptyString
              .validateNec(attr.get("ID").orElse(name).orEmpty)
              .leftMap(_ => NonEmptyChain.one(MissingXmlAttribute("ID")))

          val ucd: ValidatedNec[CatalogProblem, Ucd] = Validated
            .fromOption(attr.get("ucd"), MissingXmlAttribute("ucd"))
            .toValidatedNec
            .andThen(Ucd.apply)

          val nameV = Validated.fromOption(name, MissingXmlAttribute("name")).toValidatedNec
          ((id, ucd).mapN(FieldId.apply), nameV).mapN { (i, u) =>
            FieldDescriptor(i, u) :: f
          } match {
            case Valid(f)       => go(s, t, f, fields)
            case i @ Invalid(_) => Pull.output1(i) >> Pull.done // fail fast on field parse failure
          }
        case Some((StartTag(QName(_, "DATA"), _, _), s))        =>
          f match {
            case h :: tail =>
              go(s, t, f.reverse, NonEmptyList(h, tail).reverse.some)
            case _         =>
              Pull.output1(Validated.invalidNec(NoFieldsFound)) >> Pull.done
          }
        case Some((StartTag(QName(_, "TD"), _, _), s))          =>
          f match {
            case head :: tail =>
              go(s,
                 PartialTableRow.items.modify(PartialTableRowItem(head).asLeft :: _)(t),
                 tail,
                 fields
              )
            case Nil          =>
              Pull.output1(Validated.invalidNec(ExtraRow)) >> Pull.done
          }
        case Some((EndTag(QName(_, "TR")), s))                  =>
          f match {
            case x :: l =>
              Pull.output1(
                Validated.invalid(
                  NonEmptyChain(MissingValue(x.id), l.map(x => MissingValue(x.id)): _*)
                )
              ) >> go(s, PartialTableRow(Nil), f, fields)
            case Nil
                // this indicates we have a mismatch between fields and data
                if t.items.length =!= fields.foldMap(_.length) =>
              Pull.output1(
                Validated.invalid(
                  fields match {
                    case Some(l) => NonEmptyChain.fromNonEmptyList(l.map(x => MissingValue(x.id)))
                    case None    => NonEmptyChain.one(MissingRow)
                  }
                )
              ) >> Pull.done
            case _      =>
              Pull.output1(Validated.validNec(t.toTableRow)) >> go(s,
                                                                   PartialTableRow(Nil),
                                                                   fields.foldMap(_.toList),
                                                                   fields
              )
          }
        case Some((XmlString(v, _), s))                         =>
          go(s,
             PartialTableRow.items
               .composeOptional(listIndex.index(0))
               .modify {
                 case ti @ Right(_) => ti
                 case Left(pti)     => TableRowItem(pti.field, v).asRight
               }(t),
             f,
             fields
          )
        case Some((_, s))                                       =>
          go(s, t, f, fields)
        case None                                               => Pull.done
      }
    in => go(in, PartialTableRow(Nil), Nil, None).stream
  }

  /**
   * Function to convert a TableRow to a target using a given adapter
   */
  protected def targetRow2Target(
    adapter: CatalogAdapter,
    row:     TableRow
  ): ValidatedNec[CatalogProblem, Target] = {
    val entries = row.itemsMap

    def parseId: ValidatedNec[CatalogProblem, NonEmptyString] =
      Validated
        .fromOption(entries.get(adapter.idField).flatMap(refineV[NonEmpty](_).toOption),
                    MissingValue(adapter.idField)
        )
        .toValidatedNec

    def parseName: ValidatedNec[CatalogProblem, NonEmptyString] =
      Validated
        .fromOption(entries.get(adapter.nameField).flatMap(refineV[NonEmpty](_).toOption),
                    MissingValue(adapter.nameField)
        )
        .toValidatedNec

    def parseDoubleDegrees(ucd: Ucd, v: String): ValidatedNec[CatalogProblem, Angle] =
      parseDoubleValue(ucd, v).map(Angle.fromDoubleDegrees)

    def parseDec: ValidatedNec[CatalogProblem, Declination] =
      Validated
        .fromOption(entries.get(adapter.decField), MissingValue(adapter.decField))
        .toValidatedNec
        .andThen(parseDoubleDegrees(VoTableParser.UCD_DEC, _))
        .map(Declination.fromAngleWithCarry(_)._1)

    def parseRA: ValidatedNec[CatalogProblem, RightAscension] =
      Validated
        .fromOption(entries.get(adapter.raField), MissingValue(adapter.raField))
        .toValidatedNec
        .andThen(parseDoubleDegrees(VoTableParser.UCD_RA, _))
        .map(a => RightAscension.fromDoubleDegrees(a.toDoubleDegrees))

    val parsePV = adapter.parseProperVelocity(entries)

    def parseEpoch: ValidatedNec[CatalogProblem, Epoch] =
      Validated.validNec(
        entries.get(adapter.epochField).flatMap(Epoch.fromString.getOption).getOrElse(Epoch.J2000)
      )

    def parsePlx: ValidatedNec[CatalogProblem, Option[Parallax]] =
      entries.get(adapter.plxField) match {
        case Some(p) =>
          parseDoubleValue(VoTableParser.UCD_PLX, p)
            .map(p => Parallax.milliarcseconds.reverseGet(p).some)
        case _       =>
          Validated.validNec(none)
      }

    // Read readial velocity. if not found it will try to get it from redshift
    def parseRadialVelocity: ValidatedNec[CatalogProblem, Option[RadialVelocity]] = {
      def rvFromZ(z: String) =
        parseDoubleValue(VoTableParser.UCD_Z, z)
          .map(z => Redshift(z).toRadialVelocity)

      def fromRV(rv: String) =
        parseDoubleValue(VoTableParser.UCD_RV, rv)
          .map(rv => RadialVelocity(rv.withUnit[KilometersPerSecond]))

      (entries.get(adapter.rvField), entries.get(adapter.zField)) match {
        case (Some(rv), Some(z)) =>
          fromRV(rv).orElse(rvFromZ(z))
        case (Some(rv), _)       =>
          fromRV(rv)
        case (_, Some(z))        =>
          rvFromZ(z)
        case _                   =>
          Validated.validNec(none)
      }
    }

    def parseProperMotion: ValidatedNec[CatalogProblem, ProperMotion] =
      (parseRA, parseDec, parseEpoch, parsePV, parseRadialVelocity, parsePlx).mapN {
        (ra, dec, epoch, pv, rv, plx) =>
          ProperMotion(Coordinates(ra, dec), epoch, pv, rv, plx)
      }

    val parseMagnitudes = adapter.parseMagnitudes(entries)

    def parseSiderealTarget: ValidatedNec[CatalogProblem, Target] =
      (parseName, parseId, parseProperMotion, parseMagnitudes).mapN { (name, id, pm, mags) =>
        Target(name, id.some, pm.asRight, SortedMap(mags.fproductLeft(_.band): _*))
      }

    parseSiderealTarget.leftMap(x => x.map(u => println(u.displayValue)))
    parseSiderealTarget
  }

  /**
   * FS2 pipe to convert a stream of xml events to targets
   */
  def xml2targets[F[_]](
    adapter: CatalogAdapter
  ): Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, Target]] = {
    def go(
      s: Stream[F, ValidatedNec[CatalogProblem, TableRow]]
    ): Pull[F, ValidatedNec[CatalogProblem, Target], Unit] =
      s.pull.uncons1.flatMap {
        case Some((i @ Validated.Invalid(_), s))       => Pull.output1(i) >> go(s)
        case Some((Validated.Valid(row: TableRow), s)) =>
          Pull.output1(targetRow2Target(adapter, row)) >> go(s)
        case _                                         => Pull.done
      }
    in => go(in.through(trsf[F])).stream
  }

  /**
   * FS2 pipe to convert a stream of String to targets
   */
  def targets[F[_]: RaiseThrowable: MonadError[*[_], Throwable]](
    catalog: CatalogName
  ): Pipe[F, String, ValidatedNec[CatalogProblem, Target]] =
    in =>
      in.flatMap(Stream.emits(_))
        .through(events[F])
        .through(referenceResolver[F]())
        .through(normalize[F])
        .through(xml2targets[F](CatalogAdapter.forCatalog(catalog)))
}
