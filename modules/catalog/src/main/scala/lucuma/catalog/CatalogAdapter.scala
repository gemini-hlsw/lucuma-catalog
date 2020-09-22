// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import scala.xml._

import cats.data.Validated._
import cats.data._
import cats.implicits._
import coulomb._
import lucuma.catalog._
import lucuma.core.enum.MagnitudeBand
import lucuma.core.enum.MagnitudeSystem
import lucuma.core.math.MagnitudeValue
import lucuma.core.math.ProperVelocity
import lucuma.core.math.ProperVelocity.AngularVelocityComponent
import lucuma.core.math.RadialVelocity
import lucuma.core.math.VelocityAxis
import lucuma.core.math.units._
import lucuma.core.model.Magnitude
import monocle.state.all._

// A CatalogAdapter improves parsing handling catalog-specific options like parsing magnitudes and selecting key fields
sealed trait CatalogAdapter {

  /** Identifies the catalog to which the adapter applies. */
  def catalog: CatalogName

  // Required fields
  def idField: FieldId
  def raField: FieldId
  def decField: FieldId
  def epochField = FieldId.unsafeFrom("ref_epoch", VoTableParser.UCD_EPOCH)
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
    val idField                  = FieldId.unsafeFrom("MAIN_ID", VoTableParser.UCD_OBJID)
    val raField                  = FieldId.unsafeFrom("RA_d", VoTableParser.UCD_RA)
    val decField                 = FieldId.unsafeFrom("DEC_d", VoTableParser.UCD_DEC)
    override val pmRaField       = FieldId("PMRA", VoTableParser.UCD_PMRA)
    override val pmDecField      = FieldId("PMDEC", VoTableParser.UCD_PMDEC)

    override def ignoreMagnitudeField(v: FieldId): Boolean =
      !v.id.value.toLowerCase.startsWith("flux") ||
        v.id.value.matches(errorFluxIDExtra) ||
        v.id.value.matches(fluxIDExtra)

    override def isMagnitudeSystemField(v: (FieldId, String)): Boolean =
      v._1.id.value.toLowerCase.startsWith("flux_system")

    // Simbad has a few special cases to map sloan magnitudes
    def findBand(id: FieldId): Option[MagnitudeBand] =
      (id.id.value, id.ucd) match {
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
        errorFluxID.findFirstIn(v._1.id.value).isDefined &&
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
          p._1.id.value match {
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
