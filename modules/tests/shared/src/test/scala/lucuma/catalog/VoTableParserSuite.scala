// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.MonadError
import cats.data.NonEmptyChain
import cats.data.Validated
import cats.effect._
import fs2._
import fs2.data.xml._
import lucuma.catalog.CatalogProblem._
import lucuma.catalog._
import munit.CatsEffectSuite

import scala.xml.Node
import scala.xml.Utility

class VoTableParserSuite extends CatsEffectSuite with VoTableParser with VoTableSamples {
  def toStream[F[_]: MonadError[*[_], Throwable]](xml: Node): Stream[F, XmlEvent] =
    Stream
      .emits(Utility.trim(xml).toString)
      .through(events[F, Char])
      .through(referenceResolver[F]())
      .through(normalize[F])

  test("be able to parse a field definition") {
    val fieldXml =
      <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
    toStream[IO](fieldXml).through(fd).compile.lastOrError.map { r =>
      assertEquals(
        r,
        Validated.validNec(
          FieldId.unsafeFrom("gmag_err", Ucd.unsafeFromString("stat.error;phot.mag;em.opt.g"))
        )
      )
    }
  }
  test("ignore empty fields") {
    // Empty field
    toStream[IO](<FIELD/>)
      .through(fd)
      .compile
      .lastOrError
      .map(
        assertEquals(
          _,
          Validated.invalid(NonEmptyChain(MissingXmlAttribute("ID"), MissingXmlAttribute("ucd")))
        )
      )
  }
  test("non field xml") {
    // non field xml
    toStream[IO](<TAG/>)
      .through(fd)
      .compile
      .lastOrError
      .map(assertEquals(_, Validated.invalidNec(UnknownXmlTag("TAG"))))
  }
  test("missing attributes") {
    // missing attributes
    toStream[IO](<FIELD ID="abc"/>)
      .through(fd)
      .compile
      .lastOrError
      .map(
        assertEquals(_, Validated.invalid(NonEmptyChain(MissingXmlAttribute("ucd"))))
      )
  }
  test("swap in name for ID if missing in a field definition") {
    val fieldXml = <FIELD datatype="double" name="ref_epoch" ucd="meta.ref;time.epoch" unit="yr"/>
    toStream[IO](fieldXml)
      .through(fd)
      .compile
      .lastOrError
      .map(
        assertEquals(
          _,
          Validated.validNec(
            FieldId.unsafeFrom("ref_epoch", Ucd.unsafeFromString("meta.ref;time.epoch"))
          )
        )
      )
  }
  test("parse a list of fields") {
    val result =
      List(
        FieldId.unsafeFrom("gmag_err", Ucd.unsafeFromString("stat.error;phot.mag;em.opt.g")),
        FieldId.unsafeFrom("rmag_err", Ucd.unsafeFromString("stat.error;phot.mag;em.opt.r")),
        FieldId.unsafeFrom("flags1", Ucd.unsafeFromString("meta.code")),
        FieldId.unsafeFrom("ppmxl", Ucd.unsafeFromString("meta.id;meta.main"))
      )

    toStream[IO](fieldsNode)
      .through(fds)
      .compile
      .lastOrError
      .map(assertEquals(_, result.map(Validated.validNec)))
  }
  test("parse a data row with a list of fields") {
    val fields = toStream[IO](fieldsNode).through(fds)

    val result = TableRow(
      List(
        TableRowItem(FieldId.unsafeFrom("gmag_err", "stat.error;phot.mag;em.opt.g"), "0.0960165"),
        TableRowItem(FieldId.unsafeFrom("rmag_err", "stat.error;phot.mag;em.opt.r"), "0.0503736"),
        TableRowItem(FieldId.unsafeFrom("flags1", "meta.code"), "268435728"),
        TableRowItem(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"), "-2140405448")
      )
    )
    fields
      .flatMap { fields =>
        val fl = fields.collect { case Validated.Valid(f) => f }
        toStream[IO](tableRow).through(tr(fl))
      }
      .compile
      .lastOrError
      .map(assertEquals(_, Validated.validNec(result)))
  }
  test("detect missing fields") {
    val fields = toStream[IO](fieldsNode).through(fds)

    fields
      .flatMap { fields =>
        val fl = fields.collect { case Validated.Valid(f) => f }
        toStream[IO](tableRowMissing).through(tr(fl))
      }
      .compile
      .lastOrError
      .map(
        assertEquals(_,
                     Validated.invalidNec[CatalogProblem, TableRow](
                       MissingValue(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"))
                     )
        )
      )
  }
  test("detect extra fields") {
    val fields = toStream[IO](fieldsNode).through(fds)

    fields
      .flatMap { fields =>
        val fl = fields.collect { case Validated.Valid(f) => f }
        toStream[IO](tableRowExtra).through(tr(fl))
      }
      .compile
      .lastOrError
      .map(assertEquals(_, Validated.invalidNec[CatalogProblem, TableRow](ExtraRow)))
  }
  test("parse a list of rows with a list of fields") {
    val fields = toStream[IO](fieldsNode).through(fds)

    val result = List(
      TableRow(
        List(
          TableRowItem(FieldId.unsafeFrom("gmag_err", "stat.error;phot.mag;em.opt.g"), "0.0960165"),
          TableRowItem(FieldId.unsafeFrom("rmag_err", "stat.error;phot.mag;em.opt.r"), "0.0503736"),
          TableRowItem(FieldId.unsafeFrom("flags1", "meta.code"), "268435728"),
          TableRowItem(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"), "-2140405448")
        )
      ),
      TableRow(
        List(
          TableRowItem(FieldId.unsafeFrom("gmag_err", "stat.error;phot.mag;em.opt.g"), "0.51784"),
          TableRowItem(FieldId.unsafeFrom("rmag_err", "stat.error;phot.mag;em.opt.r"), "0.252201"),
          TableRowItem(FieldId.unsafeFrom("flags1", "meta.code"), "536871168"),
          TableRowItem(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"), "-2140404569")
        )
      )
    )

    fields
      .flatMap { fields =>
        val fl = fields.collect { case Validated.Valid(f) => f }
        toStream[IO](dataNode).through(trs(fl))
      }
      .compile
      .toList
      .map(assertEquals(_, result.map(Validated.validNec)))
  }
  test("parse a list of rows with a list of fields and one missing row") {
    val fields = toStream[IO](fieldsNode).through(fds)

    val result = List(
      Validated.invalidNec(MissingValue((FieldId.unsafeFrom("ppmxl", "meta.id;meta.main")))),
      Validated.validNec(
        TableRow(
          TableRowItem(FieldId.unsafeFrom("gmag_err", "stat.error;phot.mag;em.opt.g"), "0.51784") ::
            TableRowItem(FieldId.unsafeFrom("rmag_err", "stat.error;phot.mag;em.opt.r"),
                         "0.252201"
            ) ::
            TableRowItem(FieldId.unsafeFrom("flags1", "meta.code"), "536871168") ::
            TableRowItem(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"), "-2140404569") :: Nil
        )
      )
    )

    fields
      .flatMap { fields =>
        val fl = fields.collect { case Validated.Valid(f) => f }
        toStream[IO](dataNodeMissing).through(trs(fl))
      }
      .compile
      .toList
      .map(assertEquals(_, result))
  }
  test("parse a list of rows") {
    val result = List(
      Validated.validNec(
        TableRow(
          List(
            TableRowItem(FieldId.unsafeFrom("flags1", "meta.code"), "268435728"),
            TableRowItem(FieldId.unsafeFrom("umag", "phot.mag;em.opt.u"), "23.0888"),
            TableRowItem(FieldId.unsafeFrom("flags2", "meta.code"), "8208"),
            TableRowItem(FieldId.unsafeFrom("imag", "phot.mag;em.opt.i"), "20.3051"),
            TableRowItem(FieldId.unsafeFrom("decj2000", "pos.eq.dec;meta.main"), "0.209323681906"),
            TableRowItem(FieldId.unsafeFrom("raj2000", "pos.eq.ra;meta.main"), "359.745951955"),
            TableRowItem(FieldId.unsafeFrom("rmag", "phot.mag;em.opt.r"), "20.88"),
            TableRowItem(FieldId.unsafeFrom("objid", "meta.id;meta.main"), "-2140405448"),
            TableRowItem(FieldId.unsafeFrom("gmag", "phot.mag;em.opt.g"), "22.082"),
            TableRowItem(FieldId.unsafeFrom("zmag", "phot.mag;em.opt.z"), "19.8812"),
            TableRowItem(FieldId.unsafeFrom("type", "meta.code"), "3"),
            TableRowItem(FieldId.unsafeFrom("ppmxl", "meta.id;meta.main"), "-2140405448")
          )
        )
      )
    )

    toStream[IO](targets)
      .through(trsf)
      .compile
      .toList
      .map(v => assertEquals(v.headOption, result.headOption))
  }
}
