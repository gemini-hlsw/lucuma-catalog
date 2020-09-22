// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.data.Validated._
import cats.data._
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.cats.syntax._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import fs2._
import fs2.data.xml.Attr
import fs2.data.xml.QName
import fs2.data.xml.XmlEvent
import fs2.data.xml.XmlEvent.EndTag
import fs2.data.xml.XmlEvent.StartTag
import fs2.data.xml.XmlEvent.XmlString
import lucuma.catalog._
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.RightAscension
import lucuma.core.model.Magnitude
import lucuma.core.model.Target
import monocle.function.Index.listIndex
import monocle.macros.Lenses

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
  // type CatalogResult = Either[CatalogProblem, ParsedVoResource]

  // by band
  val MagnitudeOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(_.band)

  val UCD_OBJID      = Ucd.unsafeFromString("meta.id;meta.main")
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

  import scala.xml.Node
  def fd[F[_]]: Pipe[F, XmlEvent, ValidatedNec[CatalogProblem, FieldDescriptor]] = {
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

  def fds[F[_]]: Pipe[F, XmlEvent, List[ValidatedNec[CatalogProblem, FieldDescriptor]]] = {
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

  protected def parseTableRow(fields: List[FieldDescriptor], xml: Node): TableRow = {
    val rows = for {
      tr <- xml \\ "TR"
      td  = tr \ "TD"
      if td.length == fields.length
    } yield for {
      f <- fields.zip(td)
    } yield TableRowItem(f._1, f._2.text)
    TableRow(rows.flatten.toList)
  }

  protected def parseTableRows(fields: List[FieldDescriptor], xml: Node) =
    for {
      table <- xml \\ "TABLEDATA"
      tr    <- table \\ "TR"
    } yield parseTableRow(fields, tr)

  /**
   * Takes an XML Node and attempts to extract the resources and targets from a VOBTable
   */
  // protected def parseNode(adapter: CatalogAdapter, xml: Node): ParsedVoResource[List] =
  //   ParsedVoResource(
  //     (xml \\ "TABLE").toList.map { table =>
  //       val fields = parseFields(table)
  //       val rows   = parseTableRows(fields, table).toList
  //
  //       ParsedTable(rows.traverse(tableRow2Target(adapter, fields)))
  //     }
  //   )

  /**
   * Convert a table row to a sidereal target or CatalogProblem
   */
  protected def tableRow2Target(adapter: CatalogAdapter, fields: List[FieldDescriptor])(
    row:                                 TableRow
  ): ValidatedNel[CatalogProblem, Target] = {
    println(fields)
    val entries = row.itemsMap

    def parseEpoch: ValidatedNel[CatalogProblem, Epoch] =
      Validated.validNel(
        entries.get(adapter.epochField).flatMap(Epoch.fromString.getOption).getOrElse(Epoch.J2000)
      )
    // e.fold(Epoch.J2000.validNel[CatalogProblem]) { s =>
    //   CatalogAdapter.parseDoubleValue(VoTableParser.UCD_EPOCH, s).map(Epoch.fromString.getOption)
    // }

    // def parseProperMotion(
    //   pm:    (Option[String], Option[String]),
    //   epoch: Option[String]
    // ): ValidatedNel[CatalogProblem, Option[ProperMotion]] =
    //   (pm._1.filter(_.nonEmpty), pm._2.filter(_.nonEmpty)).mapN { (pmra, pmdec) =>
    //     for {
    //       pv <- adapter.parseProperVelocity(pmra, pmdec)
    //       e  <- parseEpoch(epoch)
    //     } yield ProperMotion(pmra, pmdec, e)
    //   }.sequenceU
    //
    // def parseDoubleVal[A](
    //   s: Option[String],
    //   u: Ucd
    // )(
    //   f: Double => CatalogProblem \/ A
    // ): CatalogProblem \/ Option[A] =
    //   s.filter(_.nonEmpty).traverseU { ds =>
    //     for {
    //       d <- CatalogAdapter.parseDoubleValue(u, ds)
    //       a <- f(d)
    //     } yield a
    //   }

    // def parseZ(z: Option[String]): CatalogProblem \/ Option[Redshift] =
    //   parseDoubleVal(z, VoTableParser.UCD_Z) { d =>
    //     Redshift(d).right[CatalogProblem]
    //   }
    //
    // def parseRv(kps: Option[String]): CatalogProblem \/ Option[Redshift] =
    //   parseDoubleVal(kps, VoTableParser.UCD_RV) { d =>
    //     if (d.abs > Redshift.C.toKilometersPerSecond)
    //       GenericError("Invalid radial velocity: " + kps).left
    //     else Redshift.fromRadialVelocityJava(d).right
    //   }
    //
    // def parsePlx(plx: Option[String]): CatalogProblem \/ Option[Parallax] =
    //   parseDoubleVal(plx, VoTableParser.UCD_PLX)(d => Parallax(0.0.max(d)).right)
    //
    def parseId: ValidatedNel[CatalogProblem, String] =
      Validated
        .fromOption(entries.get(adapter.idField), MissingValue(adapter.idField))
        .toValidatedNel

    def parseDoubleDegrees(ucd: Ucd, v: String): ValidatedNel[CatalogProblem, Angle] =
      CatalogAdapter.parseDoubleValue(ucd, v).map(Angle.fromDoubleDegrees)

    def parseDec: ValidatedNel[CatalogProblem, Declination] =
      Validated
        .fromOption(entries.get(adapter.raField), MissingValue(adapter.raField))
        .toValidatedNel
        .andThen(parseDoubleDegrees(VoTableParser.UCD_DEC, _))
        .andThen(a =>
          Validated
            .fromOption(
              Declination.fromAngle.getOption(a),
              FieldValueProblem(VoTableParser.UCD_DEC, a.toString)
            )
            .toValidatedNel
        )

    def parseRA: ValidatedNel[CatalogProblem, RightAscension] =
      Validated
        .fromOption(entries.get(adapter.raField), MissingValue(adapter.raField))
        .toValidatedNel
        .andThen(parseDoubleDegrees(VoTableParser.UCD_RA, _))
        .andThen(a =>
          Validated
            .fromOption(
              RightAscension.fromAngleExact.getOption(a),
              FieldValueProblem(VoTableParser.UCD_RA, a.toString)
            )
            .toValidatedNel
        )

    def parseProperMotion: ValidatedNel[CatalogProblem, ProperMotion] =
      (parseRA, parseDec, parseEpoch).mapN { (ra, dec, epoch) =>
        ProperMotion(Coordinates(ra, dec), epoch, None, None, None)
      }

    // def parseId: ValidatedNel[CatalogProblem, String] =
    //   Validated
    //     .fromOption(entries.get(adapter.idField), MissingValue(adapter.idField))
    //     .toValidatedNel
    //
    // def toSiderealTarget(
    //   id:    String,
    //   ra:    String,
    //   dec:   String,
    //   pm:    (Option[String], Option[String]),
    //   epoch: Option[String],
    //   z:     Option[String],
    //   kps:   Option[String],
    //   plx:   Option[String]
    // ): ValidatedNel[CatalogProblem, Target] =
    // for {
    //   r            <- Angle.parseDegrees(ra).leftMap(_ => FieldValueProblem(VoTableParser.UCD_RA, ra))
    //   d            <- Angle.parseDegrees(dec).leftMap(_ => FieldValueProblem(VoTableParser.UCD_DEC, dec))
    //   declination  <- Declination.fromAngle(d) \/> FieldValueProblem(VoTableParser.UCD_DEC, dec)
    //   properMotion <- parseProperMotion(pm, epoch)
    //   redshift0    <- parseZ(z)
    //   redshift1    <- parseRv(kps)
    //   parallax     <- parsePlx(plx)
    //   mags         <- adapter.parseMagnitudes(entries)
    // } yield SiderealTarget(
    //   id,
    //   Coordinates(RightAscension.fromAngle(r), declination),
    //   properMotion,
    //   redshift0.orElse(redshift1),
    //   parallax,
    //   mags,
    //   None,
    //   None
    // )
    def parseSiderealTarget: ValidatedNel[CatalogProblem, Target] =
      (parseId, parseProperMotion).mapN { (id, pm) =>
        Target(id, pm.asRight)
      }
//     for {
//       id <- Validated
//               .fromOption(entries.get(adapter.idField), MissingValue(adapter.idField))
//               .toValidatedNel
//       ra   <- entries.get(adapter.raField) \/> MissingValue(adapter.raField)
//       dec  <- entries.get(adapter.decField) \/> MissingValue(adapter.decField)
//       pmRa  = entries.get(adapter.pmRaField)
//       pmDec = entries.get(adapter.pmDecField)
//       epoch = entries.get(adapter.epochField)
//       // z     = entries.get(adapter.zField)
//       // kps   = entries.get(adapter.rvField)
//       // plx   = entries.get(adapter.plxField)
//       t    <- toSiderealTarget(id, ra, dec, (pmRa, pmDec), epoch, z, kps, plx)
//     } yield t
//
    // }

    parseSiderealTarget
  }
}
