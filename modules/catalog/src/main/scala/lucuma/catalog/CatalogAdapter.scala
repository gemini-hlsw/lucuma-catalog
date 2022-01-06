// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.data.Validated._
import cats.data._
import cats.implicits._
import coulomb._
import lucuma.catalog.CatalogProblem._
import lucuma.catalog._
import lucuma.core.enum.Band
import lucuma.core.enum.CatalogName
import lucuma.core.math.BrightnessUnits._
import lucuma.core.math.BrightnessValue
import lucuma.core.math.ProperMotion
import lucuma.core.math.ProperMotion.AngularVelocityComponent
import lucuma.core.math.RadialVelocity
import lucuma.core.math.VelocityAxis
import lucuma.core.math.dimensional._
import lucuma.core.math.units._

// A CatalogAdapter improves parsing handling catalog-specific options like parsing brightnesses and selecting key fields
sealed trait CatalogAdapter {

  /** Identifies the catalog to which the adapter applies. */
  def catalog: CatalogName

  // Required fields
  def idField: FieldId
  def nameField: FieldId
  def raField: FieldId
  def decField: FieldId
  def epochField: FieldId = FieldId.unsafeFrom("ref_epoch", VoTableParser.UCD_EPOCH)
  def pmRaField: FieldId  = FieldId.unsafeFrom("pmra", VoTableParser.UCD_PMRA)
  def pmDecField: FieldId = FieldId.unsafeFrom("pmde", VoTableParser.UCD_PMDEC)
  def zField: FieldId     = FieldId.unsafeFrom("Z_VALUE", VoTableParser.UCD_Z)
  def rvField: FieldId    = FieldId.unsafeFrom("RV_VALUE", VoTableParser.UCD_RV)
  def plxField: FieldId   = FieldId.unsafeFrom("PLX_VALUE", VoTableParser.UCD_PLX)
  def oTypeField: FieldId
  def spTypeField: FieldId
  def morphTypeField: FieldId
  def angSizeMajAxisField: FieldId
  def angSizeMinAxisField: FieldId

  // Parse nameField. In Simbad, this can include a prefix, e.g. "NAME "
  def parseName(entries: Map[FieldId, String]): Option[String] =
    entries.get(nameField)

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[Band]

  // Indicates if a field contianing a brightness value should be ignored, by default all fields are considered
  protected def ignoreBrightnessValueField(v: FieldId): Boolean

  // Attempts to extract brightness units for a particular band
  protected def parseBrightnessUnits(
    f: FieldId,
    v: String
  ): ValidatedNec[CatalogProblem, (Band, Units Of Brightness[Integrated])]

  // Indicates if the field is a brightness units field
  protected def isBrightnessUnitsField(v: (FieldId, String)): Boolean

  // Indicates if the field is a brightness value
  def isBrightnessValueField(v: (FieldId, String)): Boolean =
    containsBrightnessValue(v._1) &&
      !v._1.ucd.includes(VoTableParser.STAT_ERR) &&
      v._2.nonEmpty

  // Indicates if the field is a brightness error
  def isBrightnessErrorField(v: (FieldId, String)): Boolean =
    containsBrightnessValue(v._1) &&
      v._1.ucd.includes(VoTableParser.STAT_ERR) &&
      v._2.nonEmpty

  // filter brightnesses as a whole, removing invalid values and duplicates
  // (This is written to be overridden--see PPMXL adapter. By default nothing is done.)
  def filterAndDeduplicateBrightnesses(
    ms: Vector[(FieldId, (Band, BrightnessValue))]
  ): Vector[(Band, BrightnessValue)] =
    ms.unzip._2

  // Indicates if a parsed brightness is valid
  def validBrightness(m: BrightnessMeasure[Integrated]): Boolean =
    !(m.value.toDoubleValue.isNaN || m.error.exists(_.toDoubleValue.isNaN))

  // Attempts to extract the radial velocity of a field
  def parseRadialVelocity(ucd: Ucd, v: String): ValidatedNec[CatalogProblem, RadialVelocity] =
    parseDoubleValue(ucd, v)
      .map(v => RadialVelocity(v.withUnit[MetersPerSecond]))
      .andThen(Validated.fromOption(_, NonEmptyChain.one(FieldValueProblem(ucd, v))))

  // Attempts to extract the angular velocity of a field
  protected def parseAngularVelocity[A](
    ucd: Ucd,
    v:   String
  ): ValidatedNec[CatalogProblem, AngularVelocityComponent[A]] =
    parseDoubleValue(ucd, v)
      .map(v => AngularVelocityComponent[A](v.withUnit[MilliArcSecondPerYear]))
      .andThen(Validated.validNec(_))

  protected def parseProperMotion(
    pmra:  Option[String],
    pmdec: Option[String]
  ): ValidatedNec[CatalogProblem, Option[ProperMotion]] =
    ((pmra.filter(_.nonEmpty), pmdec.filter(_.nonEmpty)) match {
      case (a @ Some(_), None) => (a, Some("0"))
      case (None, a @ Some(_)) => (Some("0"), a)
      case a                   => a
    }).mapN { (pmra, pmdec) =>
      (parseAngularVelocity[VelocityAxis.RA](VoTableParser.UCD_PMRA, pmra),
       parseAngularVelocity[VelocityAxis.Dec](VoTableParser.UCD_PMDEC, pmdec)
      ).mapN(ProperMotion(_, _))
    }.sequence

  def parseProperMotion(
    entries: Map[FieldId, String]
  ): ValidatedNec[CatalogProblem, Option[ProperMotion]] = {
    val pmRa  = entries.get(pmRaField)
    val pmDec = entries.get(pmDecField)
    parseProperMotion(pmRa, pmDec)
  }
  // Attempts to extract a band and value for a brightness from a pair of field and value
  protected[catalog] def parseBrightnessValue(
    fieldId: FieldId,
    value:   String
  ): ValidatedNec[CatalogProblem, (FieldId, Band, Double)] =
    (Validated.fromOption(fieldToBand(fieldId), UnmatchedField(fieldId.ucd)).toValidatedNec,
     parseDoubleValue(fieldId.ucd, value)
    ).mapN((fieldId, _, _))

  private def combineWithErrorsSystemAndFilter(
    v: Vector[(FieldId, Band, Double)],
    e: Vector[(FieldId, Band, Double)],
    u: Vector[(Band, Units Of Brightness[Integrated])]
  ): Vector[(Band, BrightnessMeasure[Integrated])] = {
    val values = v.map { case (f, b, d) =>
      f -> (b -> BrightnessValue.fromDouble(d))
    }

    val errors = e.map { case (_, b, d) => b -> BrightnessValue.fromDouble(d) }.toMap
    val units  = u.toMap

    // Link band brightnesses with their errors
    filterAndDeduplicateBrightnesses(values)
      .map { case (band, value) =>
        band -> units
          .getOrElse(band, band.defaultIntegrated.units)
          .withValueTagged(value, errors.get(band))
      }
      .filter { case (_, brightness) => validBrightness(brightness) }
  }

  /**
   * A default method for turning all of a table row's fields into a list of brightnesses. GAIA has
   * a different mechanism for doing this.
   * @param entries
   *   fields in a VO table row
   * @return
   *   band brightnesses data parsed from the table row
   */
  def parseBandBrightnesses(
    entries: Map[FieldId, String]
  ): ValidatedNec[CatalogProblem, Vector[(Band, BrightnessMeasure[Integrated])]] = {
    val values: ValidatedNec[CatalogProblem, Vector[(FieldId, Band, Double)]] =
      entries.toVector
        .filter(isBrightnessValueField)
        .traverse(Function.tupled(parseBrightnessValue))

    val errors: ValidatedNec[CatalogProblem, Vector[(FieldId, Band, Double)]] =
      entries.toVector
        .filter(isBrightnessErrorField)
        .traverse(Function.tupled(parseBrightnessValue))

    val units: ValidatedNec[CatalogProblem, Vector[(Band, Units Of Brightness[Integrated])]] =
      entries.toVector
        .filter(isBrightnessUnitsField)
        .traverse(Function.tupled(parseBrightnessUnits))

    (values, errors, units).mapN(combineWithErrorsSystemAndFilter).map(_.sortBy(_._1))
  }

  // Indicates if the field has a brightness value field
  protected def containsBrightnessValue(v: FieldId): Boolean =
    v.ucd.includes(VoTableParser.UCD_MAG) &&
      v.ucd.matches(CatalogAdapter.magRegex) &&
      !ignoreBrightnessValueField(v)
}

// Common methods for UCAC4 and PPMXL
trait StandardAdapter extends CatalogAdapter {

  // Find what band the field descriptor should represent, in general prefer "upper case" bands over "lower case" Sloan bands.
  // This will prefer U, R and I over u', r' and i' but will map "g" and "z" to the Sloan bands g' and z'.
  def parseBand(fieldId: FieldId, band: String): Option[Band]

  def defaultParseBand(band: String): Option[Band] =
    Band.all
      .find(_.shortName === band.toUpperCase)
      .orElse(Band.all.find(_.shortName === band))

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[Band] = {
    // Parses a UCD token to extract the band for catalogs that include the band in the UCD (UCAC4/PPMXL)
    def parseBandToken(token: String): Option[String] =
      token match {
        case CatalogAdapter.magRegex(_, null) => "UC".some
        case CatalogAdapter.magRegex(_, b)    => b.replace(".", "").some
        case _                                => none
      }

    (for {
      t <- field.ucd.tokens.toList
      b <- parseBandToken(t.value)
    } yield parseBand(field, b)).headOption.flatten
  }

  // Indicates if a field contianing a brightness value should be ignored, by default all fields are considered
  override def ignoreBrightnessValueField(v: FieldId): Boolean = false

  override def isBrightnessUnitsField(v: (FieldId, String)): Boolean =
    false
  //
  // Attempts to extract brightness units for a particular band
  protected def parseBrightnessUnits(
    f: FieldId,
    v: String
  ): ValidatedNec[CatalogProblem, (Band, Units Of Brightness[Integrated])] =
    Validated.invalidNec(UnsupportedField(f))

}

object CatalogAdapter {

  val magRegex = """(?i)em.(opt|IR)(\.\w)?""".r

  case object Simbad extends CatalogAdapter {

    val catalog: CatalogName =
      CatalogName.Simbad

    private val errorFluxIDExtra              = "FLUX_ERROR_(.)_.+"
    private val fluxIDExtra                   = "FLUX_(.)_.+"
    private val errorFluxID                   = "FLUX_ERROR_(.)".r
    private val fluxID                        = "FLUX_(.)".r
    private val magSystemID                   = "FLUX_SYSTEM_(.).*".r
    val idField                               = FieldId.unsafeFrom("MAIN_ID", VoTableParser.UCD_OBJID)
    val nameField: FieldId                    = FieldId.unsafeFrom("TYPED_ID", VoTableParser.UCD_TYPEDID)
    val raField                               = FieldId.unsafeFrom("RA_d", VoTableParser.UCD_RA)
    val decField                              = FieldId.unsafeFrom("DEC_d", VoTableParser.UCD_DEC)
    override val pmRaField                    = FieldId.unsafeFrom("PMRA", VoTableParser.UCD_PMRA)
    override val pmDecField                   = FieldId.unsafeFrom("PMDEC", VoTableParser.UCD_PMDEC)
    override val oTypeField                   = FieldId.unsafeFrom("OTYPE_S", VoTableParser.UCD_OTYPE)
    override val spTypeField                  = FieldId.unsafeFrom("SP_TYPE", VoTableParser.UCD_SPTYPE)
    override val morphTypeField               = FieldId.unsafeFrom("MORPH_TYPE", VoTableParser.UCD_MORPHTYPE)
    override val angSizeMajAxisField: FieldId =
      FieldId.unsafeFrom("GALDIM_MAJAXIS", VoTableParser.UCD_ANGSIZE_MAJ)
    override val angSizeMinAxisField: FieldId =
      FieldId.unsafeFrom("GALDIM_MINAXIS", VoTableParser.UCD_ANGSIZE_MIN)

    override def parseName(entries: Map[FieldId, String]): Option[String] =
      super.parseName(entries).map(_.stripPrefix("NAME "))

    override def ignoreBrightnessValueField(v: FieldId): Boolean =
      !v.id.value.toLowerCase.startsWith("flux") ||
        v.id.value.matches(errorFluxIDExtra) ||
        v.id.value.matches(fluxIDExtra)

    override def isBrightnessUnitsField(v: (FieldId, String)): Boolean =
      v._1.id.value.toLowerCase.startsWith("flux_system")

    // Simbad has a few special cases to map sloan band brightnesses
    def findBand(id: FieldId): Option[Band] =
      (id.id.value, id.ucd) match {
        case (magSystemID(b), _) => findBand(b)
        case (errorFluxID(b), _) => findBand(b)
        case (fluxID(b), _)      => findBand(b)
        case _                   => none
      }

    // Simbad doesn't put the band in the ucd for  brightnesses errors
    override def isBrightnessErrorField(v: (FieldId, String)): Boolean =
      v._1.ucd.includes(VoTableParser.UCD_MAG) &&
        v._1.ucd.includes(VoTableParser.STAT_ERR) &&
        errorFluxID.findFirstIn(v._1.id.value).isDefined &&
        !ignoreBrightnessValueField(v._1) &&
        v._2.nonEmpty

    protected def findBand(band: String): Option[Band] =
      Band.all.find(_.shortName === band)

    override def fieldToBand(field: FieldId): Option[Band] =
      if ((field.ucd.includes(VoTableParser.UCD_MAG) && !ignoreBrightnessValueField(field)))
        findBand(field)
      else
        none

    private val integratedBrightnessUnits: Map[String, Units Of Brightness[Integrated]] =
      Map(
        "Vega" -> implicitly[TaggedUnit[VegaMagnitude, Brightness[Integrated]]],
        "AB"   -> implicitly[TaggedUnit[ABMagnitude, Brightness[Integrated]]]
      ).view.mapValues(_.unit).toMap

    // Attempts to find the brightness units for a band
    override def parseBrightnessUnits(
      f: FieldId,
      v: String
    ): ValidatedNec[CatalogProblem, (Band, Units Of Brightness[Integrated])] = {
      val band: Option[Band] =
        if (v.nonEmpty)
          f.id.value match {
            case magSystemID(x) => findBand(x)
            case _              => None
          }
        else
          None

      (Validated.fromOption(band, UnmatchedField(f.ucd)).toValidatedNec,
       Validated
         .fromOption(integratedBrightnessUnits.get(v), UnmatchedField(f.ucd))
         .toValidatedNec
      ).mapN((_, _))
    }
  }

  def forCatalog(c: CatalogName): Option[CatalogAdapter] =
    c match {
      case CatalogName.Simbad => Simbad.some
      case _                  => none
    }
}
