// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

// import java.io.{ ByteArrayInputStream, InputStream }
// import cats._
import cats.implicits._
// import gpp.catalog.api.CatalogName

// import scala.io.Source
import scala.xml._
// import lucuma.core.math.MagnitudeValue
import lucuma.core.model.Magnitude
import cats.data._
import cats.data.Validated._
import lucuma.core.enum.MagnitudeBand
import lucuma.core.enum.MagnitudeSystem
import lucuma.core.math.RadialVelocity
import lucuma.core.math.units._
import monocle.state.all._
import lucuma.core.math.MagnitudeValue
import lucuma.core.math.Coordinates
import coulomb._
import lucuma.core.model.Target
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.ProperVelocity.AngularVelocityComponent
import lucuma.core.math.VelocityAxis
import lucuma.core.math.ProperVelocity
import lucuma.core.math.Angle
import lucuma.core.math.RightAscension
import lucuma.core.math.Declination
import lucuma.catalog._
import fs2._
import fs2.data.xml.XmlEvent
import fs2.data.xml.XmlEvent.StartTag
import fs2.data.xml.QName
import fs2.data.xml.Attr
import fs2.data.xml.XmlEvent.EndTag
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
// import sttp.client._
// import lucuma.core.enum.MagnitudeBand

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

  // private def validate(catalogName: CatalogName, xmlText: String): Either[CatalogProblem, Unit] =
  //   \/.fromTryCatchNonFatal {
  //     import javax.xml.transform.stream.StreamSource
  //     import javax.xml.validation.SchemaFactory
  //
  //     val xsd        = s"/votable-${catalogName.voTableVersion.format}.xsd"
  //     val schemaLang = "http://www.w3.org/2001/XMLSchema"
  //     val factory    = SchemaFactory.newInstance(schemaLang)
  //     val schema     = factory.newSchema(new StreamSource(getClass.getResourceAsStream(xsd)))
  //     val validator  = schema.newValidator()
  //
  //     validator.validate(
  //       new StreamSource(
  //         new ByteArrayInputStream(xmlText.getBytes(java.nio.charset.Charset.forName("UTF-8")))
  //       )
  //     )
  //   }.leftMap(_ => ValidationError(catalogName))
  //
  /**
   * parse takes an input stream and attempts to read the xml content and convert it to a VoTable resource
   */
  // def parse(
  //   catalog: CatalogName,
  //   rxml:    Node
  // ): ValidatedNel[CatalogProblem, ParsedVoResource[List]] =
  //   Validated
  //     .fromOption(CatalogAdapter.forCatalog(catalog), UnknownCatalog)
  //     .toValidatedNel
  //     .andThen { (adapter: CatalogAdapter) =>
  //       // Load in memory (Could be a problem for large responses)
  //       println(adapter)
  //       // val xmlText                                 = Source.fromInputStream(is).getLines().mkString
  //       // println(xmlText)
  //       // println(is)
  //       val i: ValidatedNel[CatalogProblem, ParsedVoResource[List]] = catalog match {
  //         case CatalogName.Simbad =>
  //           // Simbad is a special case as it is not fully votable-compliant.
  //           // We want to catch some errors at this level to simplify the parse method
  //           // that assumes we are votable compliant
  //           val xml = rxml
  //           if (CatalogAdapter.Simbad.containsExceptions(xml))
  //             Validated.invalidNel(ValidationError(catalog))
  //           else
  //             Validated.validNel(
  //           parseNode(adapter, xml)
  //         ) //.asRight
  //
  //     case _                  =>
  //       Validated.invalidNel(ValidationError(catalog))
  //     // ???
  //     // validate(catalog, xmlText).as(parse(adapter, XML.loadString(xmlText)))
  //   }
  //   i
  //   // Validated.valid(ParsedVoResource(Nil))
  // }
//   (CatalogAdapter.forCatalog(catalog) \/> UnknownCatalog).flatMap { adapter =>
// 80k    // Load in memory (Could be a problem for large responses)
//     val xmlText = Source.fromInputStream(is, "UTF-8").getLines().mkString
//
  // }
}

// A CatalogAdapter improves parsing handling catalog-specific options like parsing magnitudes and selecting key fields
sealed trait CatalogAdapter {

  /** Identifies the catalog to which the adapter applies. */
  def catalog: CatalogName

  // Required fields
  def idField: FieldId
  def raField: FieldId
  def decField: FieldId
  def epochField = FieldId("ref_epoch", VoTableParser.UCD_EPOCH)
  def pmRaField  = FieldId("pmra", VoTableParser.UCD_PMRA)
  def pmDecField = FieldId("pmde", VoTableParser.UCD_PMDEC)
  def zField     = FieldId("Z_VALUE", VoTableParser.UCD_Z)
  def rvField    = FieldId("RV_VALUE", VoTableParser.UCD_RV)
  def plxField   = FieldId("PLX_VALUE", VoTableParser.UCD_PLX)

  // Indicates if the field is a magnitude
  def isMagnitudeField(v: (FieldId, String)): Boolean =
    containsMagnitude(v._1) &&
      !v._1.ucd.includes(VoTableParser.STAT_ERR) &&
      v._2.nonEmpty

  // Indicates if the field is a magnitude error
  def isMagnitudeErrorField(v: (FieldId, String)): Boolean =
    containsMagnitude(v._1) &&
      v._1.ucd.includes(VoTableParser.STAT_ERR) &&
      v._2.nonEmpty

  // Indicates if the field is a magnitude system
  def isMagnitudeSystemField(v: (FieldId, String)): Boolean

  // filter magnitudes as a whole, removing invalid values and duplicates
  // (This is written to be overridden--see PPMXL adapter. By default nothing is done.)
  def filterAndDeduplicateMagnitudes(ms: List[(FieldId, Magnitude)]): List[Magnitude] =
    ms.unzip._2

  // Indicates if a parsed magnitude is valid
  def validMagnitude(m: Magnitude): Boolean =
    !(m.value.toDoubleValue.isNaN || m.error.exists(_.toDoubleValue.isNaN))

  // Attempts to extract a magnitude system for a particular band
  def parseMagnitudeSys(
    p: (FieldId, String)
  ): ValidatedNel[CatalogProblem, (MagnitudeBand, MagnitudeSystem)]

  // Attempts to extract the radial velocity of a field
  def parseRadialVelocity(ucd: Ucd, v: String): ValidatedNel[CatalogProblem, RadialVelocity] =
    CatalogAdapter
      .parseDoubleValue(ucd, v)
      .map(v => RadialVelocity(v.withUnit[MetersPerSecond]))
      .andThen(Validated.fromOption(_, NonEmptyList.of(FieldValueProblem(ucd, v))))

  // Attempts to extract the angular velocity of a field
  def parseAngularVelocity[A](
    ucd: Ucd,
    v:   String
  ): ValidatedNel[CatalogProblem, AngularVelocityComponent[A]] =
    CatalogAdapter
      .parseDoubleValue(ucd, v)
      .map(v => AngularVelocityComponent[A](v.withUnit[MilliArcSecondPerYear]))
      .andThen(Validated.validNel(_)) //.fromOption(_, FieldValueProblem(ucd, v)))

  def parseProperVelocity(
    pmra:  Option[String],
    pmdec: Option[String]
  ): ValidatedNel[CatalogProblem, Option[ProperVelocity]] =
    (pmra.filter(_.nonEmpty), pmdec.filter(_.nonEmpty)).mapN { (pmra, pmdec) =>
      (parseAngularVelocity[VelocityAxis.RA](VoTableParser.UCD_PMRA, pmra),
       parseAngularVelocity[VelocityAxis.Dec](VoTableParser.UCD_PMDEC, pmdec)
      ).mapN { (pmra, pmdec) =>
        ProperVelocity(pmra, pmdec)
      }
    }.sequence

  // Attempts to extract a band and value for a magnitude from a pair of field and value
  def parseMagnitude(
    fieldId: FieldId,
    value:   String
  ): ValidatedNel[CatalogProblem, (FieldId, MagnitudeBand, Double)] = {

    val band = fieldToBand(fieldId)

    (Validated.fromOption(band, UnmatchedField(fieldId.ucd)).toValidatedNel,
     CatalogAdapter.parseDoubleValue(fieldId.ucd, value)
    ).mapN { (b, v) =>
      (fieldId, b, v)
    }
  }

  private def combineWithErrorsSystemAndFilter(
    m: List[(FieldId, MagnitudeBand, Double)],
    e: List[(FieldId, MagnitudeBand, Double)],
    s: List[(MagnitudeBand, MagnitudeSystem)]
  ): List[Magnitude] = {

    val mags      = m.map { case (f, b, d) =>
      f -> new Magnitude(MagnitudeValue((d * 100).toInt), b, none, MagnitudeSystem.AB)
    }
    val magErrors = e.map { case (_, b, d) => b -> MagnitudeValue((d * 100).toInt) }.toMap
    val magSys    = s.toMap

    // Link magnitudes with their errors
    filterAndDeduplicateMagnitudes(mags)
      .map { m =>
        (for {
          _ <- Magnitude.error.assign(magErrors.get(m.band))
          _ <- Magnitude.system.assign(magSys.getOrElse(m.band, m.system))
        } yield ()).run(m).value._1
      }
      .filter(validMagnitude)
  }

  /**
   * A default method for turning all of a table row's fields into a list of
   * Magnitudes.  GAIA has a different mechanism for doing this.
   * @param entries fields in a VO table row
   * @return magnitude data parsed from the table row
   */
  def parseMagnitudes(
    entries: Map[FieldId, String]
  ): ValidatedNel[CatalogProblem, List[Magnitude]] = {
    val mags: ValidatedNel[CatalogProblem, List[(FieldId, MagnitudeBand, Double)]]    =
      entries.toList
        .filter(isMagnitudeField)
        .traverse(Function.tupled(parseMagnitude))
    val magErrs: ValidatedNel[CatalogProblem, List[(FieldId, MagnitudeBand, Double)]] =
      entries.toList
        .filter(isMagnitudeErrorField)
        .traverse(Function.tupled(parseMagnitude))
    val magSys: ValidatedNel[CatalogProblem, List[(MagnitudeBand, MagnitudeSystem)]]  =
      entries.toList
        .filter(isMagnitudeSystemField)
        .traverse(parseMagnitudeSys)

    (mags, magErrs, magSys).mapN(combineWithErrorsSystemAndFilter)
  }

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[MagnitudeBand]

  // Indicates if a field contianing a magnitude should be ignored, by default all fields are considered
  protected def ignoreMagnitudeField(v: FieldId): Boolean

  // Indicates if the field has a magnitude field
  protected def containsMagnitude(v: FieldId): Boolean =
    v.ucd.includes(VoTableParser.UCD_MAG) &&
      v.ucd.matches(CatalogAdapter.magRegex) &&
      !ignoreMagnitudeField(v)
}

// Common methods for UCAC4 and PPMXL
// trait StandardAdapter {
//
//   // Find what band the field descriptor should represent, in general prefer "upper case" bands over "lower case" Sloan bands.
//   // This will prefer U, R and I over u', r' and i' but will map "g" and "z" to the Sloan bands g' and z'.
//   def parseBand(fieldId: FieldId, band: String): Option[MagnitudeBand] =
//     MagnitudeBand.all
//       .find(_.name == band.toUpperCase)
//       .orElse(MagnitudeBand.all.find(_.name == band))
//
//   // From a Field extract the band from either the field id or the UCD
//   protected def fieldToBand(field: FieldId): Option[MagnitudeBand] = {
//     // Parses a UCD token to extract the band for catalogs that include the band in the UCD (UCAC4/PPMXL)
//     def parseBandToken(token: String): Option[String] =
//       token match {
//         case CatalogAdapter.magRegex(_, null) => "UC".some
//         case CatalogAdapter.magRegex(_, b)    => b.replace(".", "").some
//         case _                                => none
//       }
//
//     (for {
//       t <- field.ucd.tokens
//       b <- parseBandToken(t.token)
//     } yield parseBand(field, b)).headOption.flatten
//   }
//
// }

object CatalogAdapter {

  val magRegex = """(?i)em.(opt|IR)(\.\w)?""".r

  def parseDoubleValue(
    ucd: Ucd,
    s:   String
  ): ValidatedNel[CatalogProblem, Double] =
    Validated
      .catchNonFatal(s.toDouble)
      .leftMap(_ => FieldValueProblem(ucd, s))
      .toValidatedNel

  // case object UCAC4 extends CatalogAdapter with StandardAdapter {
  //
  //   val catalog: CatalogName =
  //     CatalogName.UCAC4
  //
  //   val idField  = FieldId("ucac4", VoTableParser.UCD_OBJID)
  //   val raField  = FieldId("raj2000", VoTableParser.UCD_RA)
  //   val decField = FieldId("dej2000", VoTableParser.UCD_DEC)
  //
  //   private val ucac4BadMagnitude      = 20.0
  //   private val ucac4BadMagnitudeError = 0.9.some
  //
  //   // UCAC4 ignores A-mags
  //   override def ignoreMagnitudeField(v: FieldId): Boolean =
  //     v.id === "amag" || v.id === "e_amag"
  //
  //   // Magnitudes with value 20 or error over or equal to 0.9 are invalid in UCAC4
  //   override def validMagnitude(m: Magnitude): Boolean =
  //     super.validMagnitude(m) &&
  //       m.value =/= ucac4BadMagnitude &&
  //       m.error.map(math.abs) <= ucac4BadMagnitudeError
  //
  //   // UCAC4 has a few special cases to map magnitudes, g, r and i refer to the Sloan bands g', r' and i'
  //   override def parseBand(id: FieldId, band: String): Option[MagnitudeBand] =
  //     (id.id, id.ucd) match {
  //       case ("gmag" | "e_gmag", ucd) if ucd.includes(UcdWord("em.opt.r")) => Some(MagnitudeBand._g)
  //       case ("rmag" | "e_rmag", ucd) if ucd.includes(UcdWord("em.opt.r")) => Some(MagnitudeBand._r)
  //       case ("imag" | "e_imag", ucd) if ucd.includes(UcdWord("em.opt.i")) => Some(MagnitudeBand._i)
  //       case _                                                             => super.parseBand(id, band)
  //     }
  // }

  // case object PPMXL extends CatalogAdapter with StandardAdapter {
  //
  //   val catalog: CatalogName =
  //     CatalogName.PPMXL
  //
  //   val idField  = FieldId("ppmxl", VoTableParser.UCD_OBJID)
  //   val raField  = FieldId("raj2000", VoTableParser.UCD_RA)
  //   val decField = FieldId("decj2000", VoTableParser.UCD_DEC)
  //
  //   // PPMXL may contain two representations for bands R and B, represented with ids r1mag/r2mag or b1mag/b2mac
  //   // The ids r1mag/r2mag are preferred but if they are absent we should use the alternative values
  //   val primaryMagnitudesIds   = List("r1mag", "b1mag")
  //   val alternateMagnitudesIds = List("r2mag", "b2mag")
  //   val idsMapping             = primaryMagnitudesIds.zip(alternateMagnitudesIds)
  //
  //   // Convert angular velocity to mas per year as provided by ppmxl
  //   override def parseAngularVelocity(ucd: Ucd, v: String): CatalogProblem \/ AngularVelocity =
  //     CatalogAdapter.parseDoubleValue(ucd, v).map(AngularVelocity.fromDegreesPerYear)
  //
  //   override def filterAndDeduplicateMagnitudes(
  //     magnitudeFields: List[(FieldId, Magnitude)]
  //   ): List[Magnitude] = {
  //     // Read all magnitudes, including duplicates
  //     val magMap1 = (Map.empty[String, Magnitude] /: magnitudeFields) {
  //       case (m, (FieldId(i, _), mag)) if validMagnitude(mag) => m + (i -> mag)
  //       case (m, _)                                           => m
  //     }
  //     // Now magMap1 might have double entries for R and B.  Get rid of the alternative if so.
  //     val magMap2 = (magMap1 /: idsMapping) {
  //       case (m, (id1, id2)) =>
  //         if (magMap1.contains(id1)) m - id2 else m
  //     }
  //     magMap2.values.toList
  //   }
  // }

  // case object Gaia extends CatalogAdapter {
  //
  //   val catalog: CatalogName =
  //     CatalogName.Gaia
  //
  //   override val idField    = FieldId("designation", VoTableParser.UCD_OBJID)
  //   override val raField    = FieldId("ra", VoTableParser.UCD_RA)
  //   override val decField   = FieldId("dec", VoTableParser.UCD_DEC)
  //   override val pmRaField  = FieldId("pmra", VoTableParser.UCD_PMRA)
  //   override val pmDecField = FieldId("pmdec", VoTableParser.UCD_PMDEC)
  //   override val rvField    = FieldId("radial_velocity", VoTableParser.UCD_RV)
  //   override val plxField   = FieldId("parallax", VoTableParser.UCD_PLX)
  //
  //   // These are used to derive all other magnitude values.
  //   val gMagField = FieldId("phot_g_mean_mag", Ucd("phot.mag;stat.mean;em.opt"))
  //   val bpRpField = FieldId("bp_rp", Ucd("phot.color"))
  //
  //   /**
  //     * List of all Gaia fields of interest.  These are used in forming the ADQL
  //     * query that produces the VO Table.  See VoTableClient and the GaiaBackend.
  //     */
  //   val allFields: List[FieldId] =
  //     List(
  //       idField,
  //       raField,
  //       pmRaField,
  //       decField,
  //       pmDecField,
  //       epochField,
  //       plxField,
  //       rvField,
  //       gMagField,
  //       bpRpField
  //     )
  //
  //   final case class Conversion(
  //     b:  MagnitudeBand,
  //     g:  Double,
  //     p1: Double,
  //     p2: Double,
  //     p3: Double
  //   ) {
  //
  //     /**
  //       * Convert the catalog g-mag value to a magnitude in another band.
  //       */
  //     def convert(gMag: Double, bpRp: Double): Magnitude =
  //       Magnitude(
  //         gMag + g + p1 * bpRp + p2 * Math.pow(bpRp, 2) + p3 * Math.pow(bpRp, 3),
  //         b,
  //         None,
  //         MagnitudeSystem.Vega // Note that Gaia magnitudes are in the Vega system.
  //       )
  //
  //   }
  //
  //   val conversions: List[Conversion] =
  //     List(
  //       Conversion(MagnitudeBand.V, 0.017600, 0.00686, 0.173200, 0.000000),
  //       Conversion(MagnitudeBand.R, 0.003226, -0.38330, 0.134500, 0.000000),
  //       Conversion(MagnitudeBand.I, -0.020850, -0.74190, 0.096310, 0.000000),
  //       Conversion(MagnitudeBand._r, 0.128790, -0.24662, 0.027464, 0.049465),
  //       Conversion(MagnitudeBand._i, 0.296760, -0.64728, 0.101410, 0.000000),
  //       Conversion(MagnitudeBand._g, -0.135180, 0.46245, 0.251710, -0.021349),
  //       Conversion(MagnitudeBand.K, 0.188500, -2.09200, 0.134500, 0.000000),
  //       Conversion(MagnitudeBand.H, 0.162100, -1.96800, 0.132800, 0.000000),
  //       Conversion(MagnitudeBand.J, 0.018830, -1.39400, 0.078930, 0.000000)
  //     )
  //
  //   override def isMagnitudeField(v: (FieldId, String)): Boolean =
  //     sys.error("unused")
  //
  //   override def isMagnitudeErrorField(v: (FieldId, String)): Boolean =
  //     sys.error("unused")
  //
  //   override def filterAndDeduplicateMagnitudes(ms: List[(FieldId, Magnitude)]): List[Magnitude] =
  //     sys.error("unused")
  //
  //   override def parseMagnitudeSys(
  //     p: (FieldId, String)
  //   ): CatalogProblem \/ Option[(MagnitudeBand, MagnitudeSystem)] =
  //     sys.error("unused")
  //
  //   override def parseMagnitude(
  //     p: (FieldId, String)
  //   ): CatalogProblem \/ (FieldId, MagnitudeBand, Double) =
  //     sys.error("unused")
  //
  //   override def fieldToBand(f: FieldId): Option[MagnitudeBand] =
  //     sys.error("unused")
  //
  //   override def ignoreMagnitudeField(f: FieldId): Boolean =
  //     sys.error("unused")
  //
  //   override def containsMagnitude(f: FieldId): Boolean =
  //     sys.error("unused")
  //
  //   override def parseMagnitudes(
  //     entries: Map[FieldId, String]
  //   ): CatalogProblem \/ List[Magnitude] = {
  //
  //     type Try[A] = CatalogProblem \/ A
  //
  //     def doubleValue(f: FieldId): OptionT[Try, Double] =
  //       OptionT[Try, Double](
  //         entries
  //           .get(f)
  //           .filter(_.nonEmpty)
  //           .traverseU(CatalogAdapter.parseDoubleValue(f.ucd, _))
  //       )
  //
  //     (for {
  //       gMag <- doubleValue(gMagField)
  //       bpRp <- doubleValue(bpRpField)
  //     } yield conversions.map(_.convert(gMag, bpRp))).getOrElse(Nil)
  //
  //   }
  // }

  case object Simbad extends CatalogAdapter {

    val catalog: CatalogName =
      CatalogName.Simbad

    private val errorFluxIDExtra = "FLUX_ERROR_(.)_.+"
    private val fluxIDExtra      = "FLUX_(.)_.+"
    private val errorFluxID      = "FLUX_ERROR_(.)".r
    private val fluxID           = "FLUX_(.)".r
    private val magSystemID      = "FLUX_SYSTEM_(.)".r
    val idField                  = FieldId("MAIN_ID", VoTableParser.UCD_OBJID)
    val raField                  = FieldId("RA_d", VoTableParser.UCD_RA)
    val decField                 = FieldId("DEC_d", VoTableParser.UCD_DEC)
    override val pmRaField       = FieldId("PMRA", VoTableParser.UCD_PMRA)
    override val pmDecField      = FieldId("PMDEC", VoTableParser.UCD_PMDEC)

    override def ignoreMagnitudeField(v: FieldId): Boolean =
      !v.id.toLowerCase.startsWith("flux") ||
        v.id.matches(errorFluxIDExtra) ||
        v.id.matches(fluxIDExtra)

    override def isMagnitudeSystemField(v: (FieldId, String)): Boolean =
      v._1.id.toLowerCase.startsWith("flux_system")

    // Simbad has a few special cases to map sloan magnitudes
    def findBand(id: FieldId): Option[MagnitudeBand] =
      (id.id, id.ucd) match {
        // case ("FLUX_z" | "e_gmag", ucd) if ucd.includes(UcdWord("em.opt.i")) =>
        //   Some(MagnitudeBand.SloanZ) // Special case
        // case ("FLUX_g" | "e_rmag", ucd) if ucd.includes(UcdWord("em.opt.b")) =>
        //   Some(MagnitudeBand.SloanG) // Special case
        // case ("FLUX_r" | "e_rmag", ucd) if ucd.includes(UcdWord("em.opt.R")) =>
        //   Some(MagnitudeBand.SloanR) // Special case
        case (magSystemID(b), _) => findBand(b)
        case (errorFluxID(b), _) => findBand(b)
        case (fluxID(b), _)      => findBand(b)
        case _                   => None
      }

    // Simbad doesn't put the band in the ucd for magnitude errors
    override def isMagnitudeErrorField(v: (FieldId, String)): Boolean =
      v._1.ucd.includes(VoTableParser.UCD_MAG) &&
        v._1.ucd.includes(VoTableParser.STAT_ERR) &&
        errorFluxID.findFirstIn(v._1.id).isDefined &&
        !ignoreMagnitudeField(v._1) &&
        v._2.nonEmpty

    protected def findBand(band: String): Option[MagnitudeBand] =
      MagnitudeBand.all.find(_.shortName === band)

    override def fieldToBand(field: FieldId): Option[MagnitudeBand] =
      if ((field.ucd.includes(VoTableParser.UCD_MAG) && !ignoreMagnitudeField(field)))
        findBand(field)
      else
        none

    // Attempts to find the magnitude system for a band
    override def parseMagnitudeSys(
      p: (FieldId, String)
    ): ValidatedNel[CatalogProblem, (MagnitudeBand, MagnitudeSystem)] = {
      val band: Option[MagnitudeBand] =
        if (p._2.nonEmpty)
          p._1.id match {
            case magSystemID(x) => findBand(x)
            case _              => None
          }
        else
          None
      (Validated.fromOption(band, UnmatchedField(p._1.ucd)).toValidatedNel,
       Validated.fromOption(MagnitudeSystem.fromTag(p._2), UnmatchedField(p._1.ucd)).toValidatedNel
      ).mapN((_, _))
    }

    def containsExceptions(xml: Node): Boolean =
      // The only case known is with java.lang.NullPointerException but let's make the check
      // more general
      (xml \\ "INFO" \ "@value").text.matches("java\\..*Exception")
  }

  val All: List[CatalogAdapter] =
    List(Simbad)
  // List(UCAC4, PPMXL, Gaia, Simbad)

  def forCatalog(c: CatalogName): Option[CatalogAdapter] =
    All.find(_.catalog === c)
}

trait VoTableParser {

  import scala.xml.Node

  protected def parseFieldDescriptor[F[_]](
    xml: Stream[F, XmlEvent]
  ): Stream[F, ValidatedNec[CatalogProblem, FieldDescriptor]] =
    xml.fold(Validated.invalidNec[CatalogProblem, FieldDescriptor](MissingXmlTag("FIELD"))) {
      case (_, StartTag(QName(_, "FIELD"), xmlAttr, _)) =>
        val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
        val name = attr.get("name")

        val id: ValidatedNec[CatalogProblem, String] =
          Validated
            .fromOption(attr.get("ID").orElse(name), MissingXmlAttribute("ID"))
            .toValidatedNec

        val ucd: ValidatedNec[CatalogProblem, Ucd] = Validated
          .fromOption(attr.get("ucd"), MissingXmlAttribute("ucd"))
          .toValidatedNec
          .andThen(Ucd.apply)

        val nameV = Validated.fromOption(name, MissingXmlAttribute("name")).toValidatedNec
        (id, ucd, nameV).mapN { (i, u, n) =>
          FieldDescriptor(FieldId(i, u), n)
        }
      case (s, EndTag(QName(_, "FIELD")))               => s
      case (_, StartTag(QName(_, t), _, _))             => Validated.invalidNec(UnknownXmlTag(t))
      case (s, _)                                       => s
    }

  protected def parseFields[F[_]](
    xml: Stream[F, XmlEvent]
  ): Stream[F, List[ValidatedNec[CatalogProblem, FieldDescriptor]]] =
    xml
      .fold(List.empty[ValidatedNec[CatalogProblem, FieldDescriptor]]) {
        case (m, StartTag(QName(_, "TABLE"), _, _))       =>
          m
        case (f, StartTag(QName(_, "FIELD"), xmlAttr, _)) =>
          val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
          val name = attr.get("name")

          val id: ValidatedNec[CatalogProblem, String] =
            Validated
              .fromOption(attr.get("ID").orElse(name), MissingXmlAttribute("ID"))
              .toValidatedNec

          val ucd: ValidatedNec[CatalogProblem, Ucd] = Validated
            .fromOption(attr.get("ucd"), MissingXmlAttribute("ucd"))
            .toValidatedNec
            .andThen(Ucd.apply)

          val nameV = Validated.fromOption(name, MissingXmlAttribute("name")).toValidatedNec
          (id, ucd, nameV).mapN { (i, u, n) =>
            FieldDescriptor(FieldId(i, u), n)
          } :: f
        // val attr = xmlAttr.map { case Attr(k, v) => (k.local, v.foldMap(_.render)) }.toMap
        // val name = attr.get("name")
        // (attr.get("ID").orElse(name), attr.get("ucd").flatMap(Ucd.apply), name)
        //   .mapN { (i, u, n) =>
        //     FieldDescriptor(FieldId(i, u), n) :: f
        //   }
        //   .getOrElse(f)
        case (m, EndTag(QName(_, "TABLE")))               => m.reverse
        case (m, _)                                       => m
      }

  // protected def parseFieldDescriptor(xml: Node): Option[FieldDescriptor] =
  //   xml match {
  //     case f @ <FIELD>{_*}</FIELD> =>
  //       def attr(n: String) = (f \ s"@$n").headOption.map(_.text)
  //
  //       val name = attr("name")
  //       (attr("ID").orElse(name), attr("ucd"), name).mapN { (i, u, n) =>
  //         FieldDescriptor(FieldId(i, Ucd(u)), n)
  //       }
  //
  //     case _ => None
  //   }

  // protected def parseFields(xml: Node): List[FieldDescriptor] =
  //   (for {
  //     f <- xml \\ "FIELD"
  //   } yield parseFieldDescriptor(f)).flatten.toList

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
