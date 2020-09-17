// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

// import cats.implicits._
// import lucuma.catalog.votable.VoTableSamples
// import lucuma.core.enum.MagnitudeBand
// import lucuma.catalog.votable.CatalogAdapter
// import cats.data.Validated
import lucuma.catalog.votable.VoTableParser
// import lucuma.core.math.Parallax
// import lucuma.core.model.Target
// import scala.xml.Node
// import scala.xml.XML
// import lucuma.catalog._

// import edu.gemini.catalog.api.CatalogName
// import edu.gemini.spModel.core._
// import gsp.catalog.api._

class FieldsSuite extends munit.FunSuite with VoTableParser {

  // test("be able to parse a field definition") {
  //   val fieldXml =
  //     <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
  //   assertEquals(
  //     parseFieldDescriptor(fieldXml),
  //     Some(
  //       FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err")
  //     )
  //   )
  //   // Empty field
  //   assert(parseFieldDescriptor(<FIELD/>).isEmpty)
  //   // non field xml
  //   assert(parseFieldDescriptor(<TAG/>).isEmpty)
  //   // missing attributes
  //   assert(parseFieldDescriptor(<FIELD ID="abc"/>).isEmpty)
  // }
  // test("swap in name for ID if missing in a field definition") {
  //   val fieldXml = <FIELD datatype="double" name="ref_epoch" ucd="meta.ref;time.epoch" unit="yr"/>
  //   assertEquals(
  //     parseFieldDescriptor(fieldXml),
  //     Some(FieldDescriptor(FieldId("ref_epoch", Ucd("meta.ref;time.epoch")), "ref_epoch"))
  //   )
  // }
  // // test("be able to parse a list of fields") {
  //   val result =
  //     FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err") ::
  //       FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err") ::
  //       FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1") ::
  //       FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl") :: Nil
  //
  //   assertEquals(parseFields(fieldsNode), result)
  // }
  // test("be able to parse a data  row with a list of fields") {
  //   val fields = parseFields(fieldsNode)
  //
  //   val result = TableRow(
  //     TableRowItem(
  //       FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"),
  //       "0.0960165"
  //     ) ::
  //       TableRowItem(
  //         FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"),
  //         "0.0503736"
  //       ) ::
  //       TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") ::
  //       TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"),
  //                    "-2140405448"
  //       ) :: Nil
  //   )
  //   assertEquals(parseTableRow(fields, tableRow), result)
  // }
  // test("be able to parse a list of rows with a list of fields") {
  //   val fields = parseFields(fieldsNode)
  //
  //   val result = List(
  //     TableRow(
  //       TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")),
  //                                    "gmag_err"
  //                    ),
  //                    "0.0960165"
  //       ) ::
  //         TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")),
  //                                      "rmag_err"
  //                      ),
  //                      "0.0503736"
  //         ) ::
  //         TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"),
  //                      "268435728"
  //         ) ::
  //         TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"),
  //                      "-2140405448"
  //         ) :: Nil
  //     ),
  //     TableRow(
  //       TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")),
  //                                    "gmag_err"
  //                    ),
  //                    "0.51784"
  //       ) ::
  //         TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")),
  //                                      "rmag_err"
  //                      ),
  //                      "0.252201"
  //         ) ::
  //         TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"),
  //                      "536871168"
  //         ) ::
  //         TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"),
  //                      "-2140404569"
  //         ) :: Nil
  //     )
  //   )
  //   assertEquals(parseTableRows(fields, dataNode), result)
  // }
  // test("be able to convert a TableRow into a SiderealTarget") {
  // val fields = parseFields(fieldsNode)
  //
  // val validRow = TableRow(
  //   TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"),
  //                "123456"
  //   ) ::
  //     TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")), "dej2000"),
  //                  "0.209323681906"
  //     ) ::
  //     TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"),
  //                  "359.745951955"
  //     ) :: Nil
  // )
  // assertEquals(
  //   tableRow2Target(CatalogAdapter.PPMXL, fields)(validRow),
  //   Right(
  //     SiderealTarget.empty.copy(
  //       name = "123456",
  //       coordinates = Coordinates(
  //         RightAscension.fromAngle(Angle.parseDegrees("359.745951955").getOrElse(Angle.zero)),
  //         Declination
  //           .fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero))
  //           .getOrElse(Declination.zero)
  //       )
  //     )
  //   )
  // )
  //
  // val rowWithMissingId = TableRow(
  //   TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")), "dej2000"),
  //                "0.209323681906"
  //   ) ::
  //     TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"),
  //                  "359.745951955"
  //     ) :: Nil
  // )
  // // assertEquals(tableRow2Target(CatalogAdapter.PPMXL, fields)(rowWithMissingId),
  // //              Left(MissingValue(FieldId("ppmxl", VoTableParser.UCD_OBJID)))
  // )
  //
  // val rowWithBadRa = TableRow(
  //   TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"),
  //                "123456"
  //   ) ::
  //     TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")), "dej2000"),
  //                  "0.209323681906"
  //     ) ::
  //     TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"),
  //                  "ABC"
  //     ) :: Nil
  // )
  // // assertEquals(tableRow2Target(CatalogAdapter.PPMXL, fields)(rowWithBadRa),
  // //              Left(FieldValueProblem(VoTableParser.UCD_RA, "ABC"))
  // // )
  // }
//     "be able to parse magnitude bands in PPMXL" in {
//       val iMagField = Ucd("phot.mag;em.opt.i")
//       // Optical band
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("id", iMagField), MagnitudeBand.I, 20.3051)))
//
//       val jIRMagField = Ucd("phot.mag;em.IR.J")
//       // IR band
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", jIRMagField), "13.2349")) should beEqualTo(\/-((FieldId("id", jIRMagField), MagnitudeBand.J, 13.2349)))
//
//       val jIRErrMagField = Ucd("stat.error;phot.mag;em.IR.J")
//       // IR Error
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", jIRErrMagField), "0.02")) should beEqualTo(\/-((FieldId("id", jIRErrMagField), MagnitudeBand.J, 0.02)))
//
//       // No magnitude field
//       val badField = Ucd("meta.name")
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", badField), "id")) should beEqualTo(-\/(UnmatchedField(badField)))
//
//       // Bad value
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", iMagField), "stringValue")) should beEqualTo(-\/(FieldValueProblem(iMagField, "stringValue")))
//
//       // Unknown magnitude
//       val noBandField = Ucd("phot.mag;em.opt.p")
//       CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", noBandField), "stringValue")) should beEqualTo(-\/(UnmatchedField(noBandField)))
//     }
//     "be able to map sloan magnitudes in UCAC4, OCSADV-245" in {
//       val gMagField = Ucd("phot.mag;em.opt.R")
//       // gmag maps to g'
//       CatalogAdapter.UCAC4.parseMagnitude((FieldId("gmag", gMagField), "20.3051")) should beEqualTo(\/-((FieldId("gmag", gMagField), MagnitudeBand._g, 20.3051)))
//
//       val rMagField = Ucd("phot.mag;em.opt.R")
//       // rmag maps to r'
//       CatalogAdapter.UCAC4.parseMagnitude((FieldId("rmag", rMagField), "20.3051")) should beEqualTo(\/-((FieldId("rmag", rMagField), MagnitudeBand._r, 20.3051)))
//
//       val iMagField = Ucd("phot.mag;em.opt.I")
//       // imag maps to r'
//       CatalogAdapter.UCAC4.parseMagnitude((FieldId("imag", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("imag", iMagField), MagnitudeBand._i, 20.3051)))
//     }
  // test("be able to map sloan magnitudes in Simbad") {
  //   val zMagField = Ucd("phot.mag;em.opt.I")
  //   // FLUX_z maps to z'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_z", zMagField), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_z", zMagField), MagnitudeBand.SloanZ, 20.3051))
  //   )
  //
  //   val rMagField = Ucd("phot.mag;em.opt.R")
  //   // FLUX_r maps to r'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_r", rMagField), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_r", rMagField), MagnitudeBand.SloanR, 20.3051))
  //   )
  //
  //   val uMagField = Ucd("phot.mag;em.opt.u")
  //   // FLUX_u maps to u'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_u", uMagField), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_u", uMagField), MagnitudeBand.SloanU, 20.3051))
  //   )
  //
  //   val gMagField = Ucd("phot.mag;em.opt.b")
  //   // FLUX_g maps to g'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_g", gMagField), "20.3051"),
  //     Validated.validNel(((FieldId("FLUX_g", gMagField), MagnitudeBand.SloanG, 20.3051)))
  //   )
  //
  //   val iMagField = Ucd("phot.mag;em.opt.i")
  //   // FLUX_u maps to u'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_i", iMagField), "20.3051"),
  //     Validated.validNel(((FieldId("FLUX_i", iMagField), MagnitudeBand.SloanI, 20.3051)))
  //   )
  // }
  // test("be able to map non-sloan magnitudes in Simbad") {
  //   val rMagField = Ucd("phot.mag;em.opt.R")
  //   // FLUX_R maps to R
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_R", rMagField), "20.3051"),
  //     Validated.validNel(((FieldId("FLUX_R", rMagField), MagnitudeBand.R, 20.3051)))
  //   )
  //
  //   val uMagField = Ucd("phot.mag;em.opt.U")
  //   // FLUX_U maps to U
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_U", uMagField), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_U", uMagField), MagnitudeBand.U, 20.3051))
  //   )
  //
  //   val iMagField = Ucd("phot.mag;em.opt.I")
  //   // FLUX_I maps to I
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_I", iMagField), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_I", iMagField), MagnitudeBand.I, 20.3051))
  //   )
  // }
  // test("be able to map magnitude errors in Simbad") {
  //   // Magnitude errors in simbad don't include the band in the UCD, we must get it from the ID :(
  //   val magErrorUcd = Ucd("stat.error;phot.mag")
  //   // FLUX_r maps to r'
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_ERROR_r", magErrorUcd), "20.3051"),
  //     Validated.validNel((FieldId("FLUX_ERROR_r", magErrorUcd), MagnitudeBand.SloanR, 20.3051))
  //   )
  //
  //   // FLUX_R maps to R
  //   assertEquals(
  //     CatalogAdapter.Simbad.parseMagnitude(FieldId("FLUX_ERROR_R", magErrorUcd), "20.3051"),
  //     Validated.validNel(((FieldId("FLUX_ERROR_R", magErrorUcd), MagnitudeBand.R, 20.3051)))
  //   )
  // }
//     "be able to parse an xml into a list of SiderealTargets list of rows with a list of fields" in {
//       val magsTarget1 = List(new Magnitude(23.0888, MagnitudeBand.U), new Magnitude(22.082, MagnitudeBand._g), new Magnitude(20.88, MagnitudeBand.R), new Magnitude(20.3051, MagnitudeBand.I), new Magnitude(19.8812, MagnitudeBand._z))
//       val magsTarget2 = List(new Magnitude(23.0853, MagnitudeBand.U), new Magnitude(23.0889, MagnitudeBand._g), new Magnitude(21.7686, MagnitudeBand.R), new Magnitude(20.7891, MagnitudeBand.I), new Magnitude(20.0088, MagnitudeBand._z))
//
//       val result = ParsedTable(List(
//         \/-(SiderealTarget.empty.copy(name = "-2140405448", coordinates = Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget1)),
//         \/-(SiderealTarget.empty.copy(name = "-2140404569", coordinates = Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget2))
//       ))
//       // There is only one table
//       parse(CatalogAdapter.PPMXL, voTable).tables.head should beEqualTo(result)
//       parse(CatalogAdapter.PPMXL, voTable).tables.head.containsError should beFalse
//     }
//     "be able to parse an xml into a list of SiderealTargets including redshift" in {
//       val result = parse(CatalogAdapter.PPMXL, voTableWithRedshift).tables.head
//
//       // There should be no errors
//       result.containsError should beFalse
//       val redshifts = result.rows.map(_.toOption.get.redshift.get.z)
//
//       // 2 redshift values both roughly equal to 0.000068.  One specified as
//       // redshift and the other converted from radial velocity
//       redshifts.size shouldEqual 2
//       redshifts.forall(r => (r - 0.000068).abs < 0.000001) shouldEqual true
//     }
//     "balk if the radial velocity is faster than the speed of light" in {
//       P>
//       val result = parse(CatalogAdapter.PPMXL, voTableWithRadialVelocityError).tables.head
//       result.rows.head.swap.toOption.get.displayValue.startsWith("Invalid radial velocity:") shouldEqual true
//     }
//     "be able to parse an xml into a list of SiderealTargets including magnitude errors" in {
//       val magsTarget1 = List(new Magnitude(23.0888, MagnitudeBand.U, 0.518214), new Magnitude(22.082, MagnitudeBand._g, 0.0960165), new Magnitude(20.88, MagnitudeBand.R, 0.0503736), new Magnitude(20.3051, MagnitudeBand.I, 0.0456069), new Magnitude(19.8812, MagnitudeBand._z, 0.138202), new Magnitude(13.74, MagnitudeBand.J, 0.03))
//       val magsTarget2 = List(new Magnitude(23.0853, MagnitudeBand.U, 1.20311), new Magnitude(23.0889, MagnitudeBand._g, 0.51784), new Magnitude(21.7686, MagnitudeBand.R, 0.252201), new Magnitude(20.7891, MagnitudeBand.I, 0.161275), new Magnitude(20.0088, MagnitudeBand._z, 0.35873), new Magnitude(12.023, MagnitudeBand.J, 0.02))
//
//       val result = ParsedTable(List(
//         \/-(SiderealTarget.empty.copy(name = "-2140405448", coordinates = Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget1)),
//         \/-(SiderealTarget.empty.copy(name = "-2140404569", coordinates = Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget2))
//       ))
//       parse(CatalogAdapter.PPMXL, voTableWithErrors).tables.head should beEqualTo(result)
//     }
//     "be able to parse an xml into a list of SiderealTargets including proper motion" in {
//       val magsTarget1 = List(new Magnitude(14.76, MagnitudeBand._r, MagnitudeSystem.AB))
//       val magsTarget2 = List(new Magnitude(12.983, MagnitudeBand._r, MagnitudeSystem.AB))
//       val pm1 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-10.199999999999999)), DeclinationAngularVelocity(AngularVelocity(-4.9000000000000004))).some
//       val pm2 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-7)), DeclinationAngularVelocity(AngularVelocity(-13.9))).some
//
//       val result = ParsedTable(List(
//         \/-(SiderealTarget("550-001323", Coordinates(RightAscension.fromDegrees(9.897141944444456), Declination.fromAngle(Angle.parseDegrees("19.98878944444442").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm1, None, None, magsTarget1, None, None)),
//         \/-(SiderealTarget("550-001324", Coordinates(RightAscension.fromDegrees(9.91958055555557), Declination.fromAngle(Angle.parseDegrees("19.997709722222226").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm2, None, None, magsTarget2, None, None))
//       ))
//       parse(CatalogAdapter.UCAC4, voTableWithProperMotion).tables.head should beEqualTo(result)
//     }
//
//     "be able to read Gaia results" in {
//
//       // Extract just the targets.
//       val result = parse(CatalogAdapter.Gaia, voTableGaia).tables.head.rows.sequenceU.getOrElse(Nil)
//
//       // 6 targets (i.e., and no errors)
//       result.size shouldEqual 4
//
//       val List(a, b, c, d) = result
//
//       // First two targets have no magnitudes because the necessary conversion
//       // information is not present.
//       a.magnitudes shouldEqual Nil
//       b.magnitudes shouldEqual Nil
//
//       import MagnitudeBand._
//       c.magnitudes.map(_.band).toSet shouldEqual Set(_g, _r, V, R, I, _i, K, H, J)
//
//       // Final target has everything.
//       d.magnitudes.map(_.band).toSet shouldEqual CatalogName.Gaia.supportedBands.toSet
//
//       // Even radial velocity.
//       ((d.redshift.get.z - 0.000068).abs < 0.000001) shouldEqual true
//
//       // Check conversion
//       val g     = 14.2925430
//       val bp_rp =  1.0745363
//       val v     = g + 0.0176 + bp_rp * (0.00686 + 0.1732 * bp_rp)
//       (d.magnitudes.exists { m =>
//         (m.band === V) && ((m.value - v).abs < 0.000001)
//       }) shouldEqual true
//
//       // All targets with a proper motion have epoch 2015.5
//       val e2015_5 = Epoch(2015.5)
//       result.forall(_.properMotion.forall(_.epoch == e2015_5))
//     }
//
//     "be able to validate and parse an xml from sds9" in {
//       val badXml = "votable-non-validating.xml"
//       VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$badXml")) should beEqualTo(-\/(ValidationError(CatalogName.UCAC4)))
//     }
//     "be able to detect unknown catalogs" in {
//       val xmlFile = "votable-unknown.xml"
//       val result  = VoTableParser.parse(CatalogName.GSC234, getClass.getResourceAsStream(s"/$xmlFile"))
//       result must beEqualTo(-\/(UnknownCatalog))
//     }
//     "be able to validate and parse an xml from ucac4" in {
//       val xmlFile = "votable-ucac4.xml"
//       VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
//       VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
//     }
//     "be able to validate and parse an xml from ppmxl" in {
//       val xmlFile = "votable-ppmxl.xml"
//       VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
//       VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
//     }
//     "be able to select r1mag over r2mag and b2mag when b1mag is absent in ppmxl" in {
//       val xmlFile = "votable-ppmxl.xml"
//       val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.map(TargetsTable.apply).map(_.rows).flatMap(_.find(_.name == "-1471224894")).headOption
//
//       val magR = result >>= {_.magnitudeIn(MagnitudeBand.R)}
//       magR.map(_.value) should beSome(18.149999999999999)
//       val magB = result >>= {_.magnitudeIn(MagnitudeBand.B)}
//       magB.map(_.value) should beSome(17.109999999999999)
//     }
//     "be able to ignore bogus magnitudes on ppmxl" in {
//       val xmlFile = "votable-ppmxl.xml"
//       // Check a well-known target containing invalid magnitude values an bands H, I, K and J
//       val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.map(TargetsTable.apply).map(_.rows).flatMap(_.find(_.name == "-1471224894")).headOption
//       val magH = result >>= {_.magnitudeIn(MagnitudeBand.H)}
//       val magI = result >>= {_.magnitudeIn(MagnitudeBand.I)}
//       val magK = result >>= {_.magnitudeIn(MagnitudeBand.K)}
//       val magJ = result >>= {_.magnitudeIn(MagnitudeBand.J)}
//       magH should beNone
//       magI should beNone
//       magK should beNone
//       magJ should beNone
//     }
//     "be able to filter out bad magnitudes" in {
//       val xmlFile = "fmag.xml"
//       VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       val mags = result.map(_.magnitudeIn(MagnitudeBand.R))
//       // Does not contain R as it is filtered out being magnitude 20 and error 99
//       mags should beEqualTo(\/.right(None))
//     }
//     "convert fmag to UC" in {
//       val xmlFile = "fmag.xml"
//       VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       val mags = result.map(_.magnitudeIn(MagnitudeBand.UC))
//       // Fmag gets converted to UC
//       mags should beEqualTo(\/.right(Some(Magnitude(5.9, MagnitudeBand.UC, None, MagnitudeSystem.Vega))))
//     }
//     "extract Sloan's band" in {
//       val xmlFile = "sloan.xml"
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       val gmag = result.map(_.magnitudeIn(MagnitudeBand._g))
//       // gmag gets converted to g'
//       gmag should beEqualTo(\/.right(Some(Magnitude(15.0, MagnitudeBand._g, 0.39.some, MagnitudeSystem.AB))))
//       val rmag = result.map(_.magnitudeIn(MagnitudeBand._r))
//       // rmag gets converted to r'
//       rmag should beEqualTo(\/.right(Some(Magnitude(13.2, MagnitudeBand._r, 0.5.some, MagnitudeSystem.AB))))
//       val imag = result.map(_.magnitudeIn(MagnitudeBand._i))
//       // rmag gets converted to r'
//       imag should beEqualTo(\/.right(Some(Magnitude(5, MagnitudeBand._i, 0.34.some, MagnitudeSystem.AB))))
//     }
//     "parse simbad named queries" in {
//       // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=Vega&output.format=VOTable
//       val xmlFile = "simbad-vega.xml"
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       // id and coordinates
//       result.map(_.name) should beEqualTo(\/.right("* alf Lyr"))
//       result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromDegrees(279.23473479))))
//       result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDegrees(38.78368896)).getOrElse(Declination.zero)))
//       // proper motions
//       result.map(_.properMotion.map(_.deltaRA)) should beEqualTo(\/.right(Some(RightAscensionAngularVelocity(AngularVelocity(200.94)))))
//       result.map(_.properMotion.map(_.deltaDec)) should beEqualTo(\/.right(Some(DeclinationAngularVelocity(AngularVelocity(286.23)))))
//       // redshift
//       result.map(_.redshift) should beEqualTo(\/.right(Redshift(-0.000069).some))
//       // parallax
//       result.map(_.parallax) should beEqualTo(\/.right(Parallax(130.23).some))
//       // magnitudes
//       result.map(_.magnitudeIn(MagnitudeBand.U)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.U))))
//       result.map(_.magnitudeIn(MagnitudeBand.B)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.B))))
//       result.map(_.magnitudeIn(MagnitudeBand.V)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.V))))
//       result.map(_.magnitudeIn(MagnitudeBand.R)) should beEqualTo(\/.right(Some(new Magnitude(0.07, MagnitudeBand.R))))
//       result.map(_.magnitudeIn(MagnitudeBand.I)) should beEqualTo(\/.right(Some(new Magnitude(0.10, MagnitudeBand.I))))
//       result.map(_.magnitudeIn(MagnitudeBand.J)) should beEqualTo(\/.right(Some(new Magnitude(-0.18, MagnitudeBand.J))))
//       result.map(_.magnitudeIn(MagnitudeBand.H)) should beEqualTo(\/.right(Some(new Magnitude(-0.03, MagnitudeBand.H))))
//       result.map(_.magnitudeIn(MagnitudeBand.K)) should beEqualTo(\/.right(Some(new Magnitude(0.13, MagnitudeBand.K))))
//     }
//     "parse simbad named queries with sloan magnitudes" in {
//       // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2MFGC6625&output.format=VOTable
//       val xmlFile = "simbad-2MFGC6625.xml"
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       // id and coordinates
//       result.map(_.name) should beEqualTo(\/.right("2MFGC 6625"))
//       result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromHMS(8, 23, 54.966).getOrElse(Angle.zero))))
//       result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDMS(28, 6, 21.6792).getOrElse(Angle.zero)).getOrElse(Declination.zero)))
//       // proper motions
//       result.map(_.properMotion) should beEqualTo(\/.right(None))
//       // redshift
//       result.map(_.redshift) should beEqualTo(\/.right(Redshift(0.04724).some))
//       // parallax
//       result.map(_.parallax) should beEqualTo(\/.right(None))
//       // magnitudes
//       result.map(_.magnitudeIn(MagnitudeBand._u)) should beEqualTo(\/.right(Some(new Magnitude(17.353, MagnitudeBand._u, 0.009))))
//       result.map(_.magnitudeIn(MagnitudeBand._g)) should beEqualTo(\/.right(Some(new Magnitude(16.826, MagnitudeBand._g, 0.004))))
//       result.map(_.magnitudeIn(MagnitudeBand._r)) should beEqualTo(\/.right(Some(new Magnitude(17.286, MagnitudeBand._r, 0.005))))
//       result.map(_.magnitudeIn(MagnitudeBand._i)) should beEqualTo(\/.right(Some(new Magnitude(16.902, MagnitudeBand._i, 0.005))))
//       result.map(_.magnitudeIn(MagnitudeBand._z)) should beEqualTo(\/.right(Some(new Magnitude(17.015, MagnitudeBand._z, 0.011))))
//     }
//     "parse simbad named queries with mixed magnitudes" in {
//       // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2SLAQ%20J000008.13%2B001634.6&output.format=VOTable
//       val xmlFile = "simbad-J000008.13.xml"
//       // The sample has only one row
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get
//
//       // id and coordinates
//       result.map(_.name) should beEqualTo(\/.right("2SLAQ J000008.13+001634.6"))
//       result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromHMS(0, 0, 8.136).getOrElse(Angle.zero))))
//       result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDMS(0, 16, 34.6908).getOrElse(Angle.zero)).getOrElse(Declination.zero)))
//       // proper motions
//       result.map(_.properMotion) should beEqualTo(\/.right(None))
//       // redshift
//       result.map(_.redshift) should beEqualTo(\/.right(Redshift(1.8365).some))
//       // parallax
//       result.map(_.parallax) should beEqualTo(\/.right(None))
//       // magnitudes
//       result.map(_.magnitudeIn(MagnitudeBand.B)) should beEqualTo(\/.right(Some(new Magnitude(20.35, MagnitudeBand.B))))
//       result.map(_.magnitudeIn(MagnitudeBand.V)) should beEqualTo(\/.right(Some(new Magnitude(20.03, MagnitudeBand.V))))
//       // Bands J, H and K for this target have no standard magnitude system
//       result.map(_.magnitudeIn(MagnitudeBand.J)) should beEqualTo(\/.right(Some(new Magnitude(19.399, MagnitudeBand.J, 0.073, MagnitudeSystem.AB))))
//       result.map(_.magnitudeIn(MagnitudeBand.H)) should beEqualTo(\/.right(Some(new Magnitude(19.416, MagnitudeBand.H, 0.137, MagnitudeSystem.AB))))
//       result.map(_.magnitudeIn(MagnitudeBand.K)) should beEqualTo(\/.right(Some(new Magnitude(19.176, MagnitudeBand.K, 0.115, MagnitudeSystem.AB))))
//       result.map(_.magnitudeIn(MagnitudeBand._u)) should beEqualTo(\/.right(Some(new Magnitude(20.233, MagnitudeBand._u, 0.054))))
//       result.map(_.magnitudeIn(MagnitudeBand._g)) should beEqualTo(\/.right(Some(new Magnitude(20.201, MagnitudeBand._g, 0.021))))
//       result.map(_.magnitudeIn(MagnitudeBand._r)) should beEqualTo(\/.right(Some(new Magnitude(19.929, MagnitudeBand._r, 0.021))))
//       result.map(_.magnitudeIn(MagnitudeBand._i)) should beEqualTo(\/.right(Some(new Magnitude(19.472, MagnitudeBand._i, 0.023))))
//       result.map(_.magnitudeIn(MagnitudeBand._z)) should beEqualTo(\/.right(Some(new Magnitude(19.191, MagnitudeBand._z, 0.068))))
//     }
  // test("don't allow negative parallax values") {
  // From http://simbad.u-strasbg.fr/simbad/sim-id?output.format=VOTable&Ident=HIP43018
  //   import sttp.client._
  //   import sttp.client.testing._
  //   import java.io.File
  //
  //   val destination    = new File("simbad_hip43018.xml")
  //   val testingBackend = SttpBackendStub.synchronous
  //     .whenRequestMatches(_ => true)
  //     // .thenRespondNotFound()
  //     .thenRespond(destination)
  //   // We are interested only on the first row
  //
  //   println("nthanh")
  //   // println(getClass.getResourceAsStream(s"./$xmlFile"))
  //   val asXml: Either[String, String] => Either[String, Node] =
  //     x => x.bimap(x => { println(x); x; }, XML.load)
  //   val request                                               = basicRequest.get(uri"http://archive").response(asFile(destination)) //.map(asXml))
  //   val response                                              = request.send(testingBackend)
  //   val result: Option[Target]                                = VoTableParser
  //     .parse(CatalogName.SIMBAD, response.body.getOrElse(???))
  //     .getOrElse(ParsedVoResource(List.empty[ParsedTable[List]]))
  //     .tables
  //     .flatMap(_.rows.getOrElse(Nil))
  //     .headOption
  //   // .tables
  //   // .headOption
  //   // .flatMap(_.rows.headOption)
  //   // .get
  //
  //   // parallax is reported as -0.57 by Simbad, the parser makes it a 0
  //   assertEquals(result.flatMap(Target.parallax.getOption).flatten,
  //                Parallax.fromMicroarcseconds(0).some
  //   )
  // }
//     "parse simbad with a not-found name" in {
//       val xmlFile = "simbad-not-found.xml"
//       // Simbad returns non-valid xml when an element is not found, we need to skip validation :S
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile"))
//       result must beEqualTo(\/.right(ParsedVoResource(List())))
//     }
//     "parse simbad with an npe" in {
//       val xmlFile = "simbad-npe.xml"
//       // Simbad returns non-valid xml when there is an internal error like an NPE
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile"))
//       result must beEqualTo(\/.left(ValidationError(CatalogName.SIMBAD)))
//     }
//     "ppmxl proper motion should be in mas/y. REL-2841" in {
//       val xmlFile = "votable-ppmxl-proper-motion.xml"
//       // PPMXL returns proper motion on degrees per year, it should be converted to mas/year
//       val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil))
//
//       val targets = for {
//         t <- result.tables.map(TargetsTable.apply)
//         r <- t.rows
//         if r.name == "-1201792896"
//       } yield r
//
//       val pmRA = targets.headOption >>= {_.properMotion} >>= {_.deltaRA.some}
//       val pmDec = targets.headOption >>= {_.properMotion} >>= {_.deltaDec.some}
//       pmRA must beSome(RightAscensionAngularVelocity(AngularVelocity(-1.400004)))
//       pmDec must beSome(DeclinationAngularVelocity(AngularVelocity(-7.56)))
//     }
//     "support simbad repeated magnitude entries, REL-2853" in {
//       val xmlFile = "simbad-ngc-2438.xml"
//       // Simbad returns an xml with multiple measurements of the same band, use only the first one
//       val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil))
//
//       val target = (for {
//           t <- result.tables.map(TargetsTable.apply)
//           r <- t.rows
//         } yield r).headOption
//       target.map(_.name) should beSome("NGC  2438")
//       target.map(_.magnitudeIn(MagnitudeBand.J)) should beSome(Some(new Magnitude(17.02, MagnitudeBand.J, 0.15, MagnitudeSystem.Vega)))
//     }
//   }
}
