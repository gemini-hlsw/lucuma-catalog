// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.syntax.all._
import lucuma.core.enum.Band
import lucuma.core.enum.CatalogName
import lucuma.core.geom.ShapeExpression
import lucuma.core.geom.syntax.all._
import lucuma.core.math.Coordinates
import lucuma.core.model.SiderealTracking
import org.http4s.Uri
import spire.math.Bounded

import java.time.Instant

/**
 * Represents a query on a catalog
 */
sealed trait CatalogQuery {

  /**
   * Name of the catalog for this query
   */
  def catalog: CatalogName

  /**
   * Set if a proxy (e.g. cors proxy) in needed
   */
  def proxy: Option[Uri]
}

/**
 * Name based query, e.g. Simbad
 */
case class QueryByName(id: String, proxy: Option[Uri] = None) extends CatalogQuery {
  override val catalog = CatalogName.Simbad
}

trait GaiaBrightnessADQL extends CatalogQuery {
  override val catalog = CatalogName.Gaia

  def adqlBrightness(brightnessConstraints: Option[BrightnessConstraints]): List[String] =
    brightnessConstraints.foldMap {
      case BrightnessConstraints(bands, faintness, None)             =>
        bands.bands
          .collect {
            case Band.Gaia   => CatalogAdapter.Gaia.gMagField.id
            case Band.GaiaBP => CatalogAdapter.Gaia.bpMagField.id
            case Band.GaiaRP => CatalogAdapter.Gaia.rpMagField.id
          }
          .map(bid => s"($bid > ${faintness.brightness})")
      case BrightnessConstraints(bands, faintness, Some(saturation)) =>
        bands.bands
          .collect {
            case Band.Gaia   => CatalogAdapter.Gaia.gMagField.id
            case Band.GaiaBP => CatalogAdapter.Gaia.bpMagField.id
            case Band.GaiaRP => CatalogAdapter.Gaia.rpMagField.id
          }
          .map(bid => s"($bid between ${saturation.brightness} and ${faintness.brightness})")
    }
}

/**
 * Query based on ADQL with a given geometry around base coordinates
 */
sealed trait ADQLQuery {
  def base: Coordinates
  def adqlGeom(implicit ev: ADQLInterpreter): String
  def adqlBrightness: List[String]
  def proxy: Option[Uri]
}

/**
 * Query based on ADQL with a given geometry around base coordinates
 */
final case class QueryByADQL(
  base:                  Coordinates,
  shapeConstraint:       ShapeExpression,
  brightnessConstraints: Option[BrightnessConstraints],
  proxy:                 Option[Uri] = None
) extends CatalogQuery
    with ADQLQuery
    with GaiaBrightnessADQL {
  def adqlBrightness: List[String] = adqlBrightness(brightnessConstraints)

  def adqlGeom(implicit ev: ADQLInterpreter): String = {
    implicit val si = ev.shapeInterpreter

    val r = shapeConstraint.maxSide.bisect

    f"CIRCLE('ICRS', ${(base.ra.toAngle.toDoubleDegrees)}%9.8f, ${(base.dec.toAngle.toSignedDoubleDegrees)}%9.8f, ${r.toDoubleDegrees / 2}%9.8f)"
  }
}

/**
 * Query based on ADQL with a given geometry and coordinates varying on a time range It will
 * calculate a circle centered in the middle of the targets and covering the area for both ends
 *
 * See: https://github.com/gemini-hlsw/lucuma-catalog/wiki/time-range
 */
final case class TimeRangeQueryByADQL(
  tracking:              SiderealTracking,
  timeRange:             Bounded[Instant],
  shapeConstraint:       ShapeExpression,
  brightnessConstraints: Option[BrightnessConstraints],
  proxy:                 Option[Uri] = None
) extends CatalogQuery
    with ADQLQuery
    with GaiaBrightnessADQL {
  val base = tracking.baseCoordinates

  def adqlBrightness: List[String] = adqlBrightness(brightnessConstraints)

  def adqlGeom(implicit ev: ADQLInterpreter): String = {
    implicit val si = ev.shapeInterpreter

    // Coordinates at the start and of the time range
    val start  = tracking.at(timeRange.lowerBound.a)
    val end    = tracking.at(timeRange.upperBound.a)
    // offset between them
    val offset = start.diff(end).offset
    val r      = (shapeConstraint ∪ (shapeConstraint ↗ offset)).maxSide.bisect
    // Set the base in the middle of both time ends
    val base   = start.interpolate(end, 0.5)

    f"CIRCLE('ICRS', ${(base.ra.toAngle.toDoubleDegrees)}%9.8f, ${(base.dec.toAngle.toSignedDoubleDegrees)}%9.8f, ${r.toDoubleDegrees / 2}%9.8f)"
  }
}
