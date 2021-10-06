// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.data.Validated._
import cats.data._
import cats.implicits._
import coulomb._
import lucuma.catalog.CatalogProblem._
import lucuma.catalog._
import lucuma.core.enum.CatalogName
import lucuma.core.enum.MagnitudeBand
import lucuma.core.enum.MagnitudeSystem
import lucuma.core.math.MagnitudeValue
import lucuma.core.math.ProperMotion
import lucuma.core.math.ProperMotion.AngularVelocityComponent
import lucuma.core.math.RadialVelocity
import lucuma.core.math.VelocityAxis
import lucuma.core.math.units._
import lucuma.core.model.Magnitude
import lucuma.core.optics.state.all._

// A CatalogAdapter improves parsing handling catalog-specific options like parsing magnitudes and selecting key fields
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

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[MagnitudeBand]

  // Indicates if a field contianing a magnitude should be ignored, by default all fields are considered
  protected def ignoreMagnitudeField(v: FieldId): Boolean

  // Attempts to extract a magnitude system for a particular band
  protected def parseMagnitudeSys(
    f: FieldId,
    v: String
  ): ValidatedNec[CatalogProblem, (MagnitudeBand, MagnitudeSystem)]

  // Indicates if the field is a magnitude system
  protected def isMagnitudeSystemField(v: (FieldId, String)): Boolean

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

  // filter magnitudes as a whole, removing invalid values and duplicates
  // (This is written to be overridden--see PPMXL adapter. By default nothing is done.)
  def filterAndDeduplicateMagnitudes(ms: Vector[(FieldId, Magnitude)]): Vector[Magnitude] =
    ms.unzip._2

  // Indicates if a parsed magnitude is valid
  def validMagnitude(m: Magnitude): Boolean =
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
  // Attempts to extract a band and value for a magnitude from a pair of field and value
  protected[catalog] def parseMagnitude(
    fieldId: FieldId,
    value:   String
  ): ValidatedNec[CatalogProblem, (FieldId, MagnitudeBand, Double)] =
    (Validated.fromOption(fieldToBand(fieldId), UnmatchedField(fieldId.ucd)).toValidatedNec,
     parseDoubleValue(fieldId.ucd, value)
    ).mapN((fieldId, _, _))

  private def combineWithErrorsSystemAndFilter(
    m: Vector[(FieldId, MagnitudeBand, Double)],
    e: Vector[(FieldId, MagnitudeBand, Double)],
    s: Vector[(MagnitudeBand, MagnitudeSystem)]
  ): Vector[Magnitude] = {
    val mags = m.map { case (f, b, d) =>
      f -> new Magnitude(MagnitudeValue.fromDouble(d), b, none, MagnitudeSystem.AB)
    }

    val magErrors = e.map { case (_, b, d) => b -> MagnitudeValue.fromDouble(d) }.toMap
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
   * A default method for turning all of a table row's fields into a list of Magnitudes. GAIA has a
   * different mechanism for doing this.
   * @param entries
   *   fields in a VO table row
   * @return
   *   magnitude data parsed from the table row
   */
  def parseMagnitudes(
    entries: Map[FieldId, String]
  ): ValidatedNec[CatalogProblem, Vector[Magnitude]] = {
    val mags: ValidatedNec[CatalogProblem, Vector[(FieldId, MagnitudeBand, Double)]] =
      entries.toVector
        .filter(isMagnitudeField)
        .traverse(Function.tupled(parseMagnitude))

    val magErrs: ValidatedNec[CatalogProblem, Vector[(FieldId, MagnitudeBand, Double)]] =
      entries.toVector
        .filter(isMagnitudeErrorField)
        .traverse(Function.tupled(parseMagnitude))

    val magSys: ValidatedNec[CatalogProblem, Vector[(MagnitudeBand, MagnitudeSystem)]] =
      entries.toVector
        .filter(isMagnitudeSystemField)
        .traverse(Function.tupled(parseMagnitudeSys))

    (mags, magErrs, magSys).mapN(combineWithErrorsSystemAndFilter).map(_.sortBy(_.band))
  }

  // Indicates if the field has a magnitude field
  protected def containsMagnitude(v: FieldId): Boolean =
    v.ucd.includes(VoTableParser.UCD_MAG) &&
      v.ucd.matches(CatalogAdapter.magRegex) &&
      !ignoreMagnitudeField(v)
}

// Common methods for UCAC4 and PPMXL
trait StandardAdapter extends CatalogAdapter {

  // Find what band the field descriptor should represent, in general prefer "upper case" bands over "lower case" Sloan bands.
  // This will prefer U, R and I over u', r' and i' but will map "g" and "z" to the Sloan bands g' and z'.
  def parseBand(fieldId: FieldId, band: String): Option[MagnitudeBand]

  def defaultParseBand(band: String): Option[MagnitudeBand] =
    MagnitudeBand.all
      .find(_.shortName === band.toUpperCase)
      .orElse(MagnitudeBand.all.find(_.shortName === band))

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[MagnitudeBand] = {
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

  // Indicates if a field contianing a magnitude should be ignored, by default all fields are considered
  override def ignoreMagnitudeField(v: FieldId): Boolean = false

  override def isMagnitudeSystemField(v: (FieldId, String)): Boolean =
    false
  //
  // Attempts to extract a magnitude system for a particular band
  protected def parseMagnitudeSys(
    f: FieldId,
    v: String
  ): ValidatedNec[CatalogProblem, (MagnitudeBand, MagnitudeSystem)] =
    Validated.invalidNec(UnsupportedField(f))

}

object CatalogAdapter {

  val magRegex = """(?i)em.(opt|IR)(\.\w)?""".r

  case object Simbad extends CatalogAdapter {

    val catalog: CatalogName =
      CatalogName.Simbad

    private val errorFluxIDExtra = "FLUX_ERROR_(.)_.+"
    private val fluxIDExtra      = "FLUX_(.)_.+"
    private val errorFluxID      = "FLUX_ERROR_(.)".r
    private val fluxID           = "FLUX_(.)".r
    private val magSystemID      = "FLUX_SYSTEM_(.).*".r
    val idField                  = FieldId.unsafeFrom("MAIN_ID", VoTableParser.UCD_OBJID)
    val nameField: FieldId       = FieldId.unsafeFrom("TYPED_ID", VoTableParser.UCD_TYPEDID)
    val raField                  = FieldId.unsafeFrom("RA_d", VoTableParser.UCD_RA)
    val decField                 = FieldId.unsafeFrom("DEC_d", VoTableParser.UCD_DEC)
    override val pmRaField       = FieldId.unsafeFrom("PMRA", VoTableParser.UCD_PMRA)
    override val pmDecField      = FieldId.unsafeFrom("PMDEC", VoTableParser.UCD_PMDEC)

    override def ignoreMagnitudeField(v: FieldId): Boolean =
      !v.id.value.toLowerCase.startsWith("flux") ||
        v.id.value.matches(errorFluxIDExtra) ||
        v.id.value.matches(fluxIDExtra)

    override def isMagnitudeSystemField(v: (FieldId, String)): Boolean =
      v._1.id.value.toLowerCase.startsWith("flux_system")

    // Simbad has a few special cases to map sloan magnitudes
    def findBand(id: FieldId): Option[MagnitudeBand] =
      (id.id.value, id.ucd) match {
        case (magSystemID(b), _) => findBand(b)
        case (errorFluxID(b), _) => findBand(b)
        case (fluxID(b), _)      => findBand(b)
        case _                   => none
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
      f: FieldId,
      v: String
    ): ValidatedNec[CatalogProblem, (MagnitudeBand, MagnitudeSystem)] = {
      val band: Option[MagnitudeBand] =
        if (v.nonEmpty)
          f.id.value match {
            case magSystemID(x) => findBand(x)
            case _              => None
          }
        else
          None
      (Validated.fromOption(band, UnmatchedField(f.ucd)).toValidatedNec,
       Validated.fromOption(MagnitudeSystem.fromTag(v), UnmatchedField(f.ucd)).toValidatedNec
      ).mapN((_, _))
    }

  }

  def forCatalog(c: CatalogName): Option[CatalogAdapter] =
    c match {
      case CatalogName.Simbad => Simbad.some
      case _                  => none
    }
}
