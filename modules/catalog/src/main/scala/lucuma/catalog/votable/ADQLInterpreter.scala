// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.votable

import cats.syntax.all._
import lucuma.core.geom.ShapeInterpreter
import lucuma.core.math.Coordinates
import lucuma.core.math.Epoch

/**
 * ADQL queries are quite open thus multiple ways to construct them are possible. The interpreter
 * will take the params of a geometry based query and build restrictions
 */
trait ADQLInterpreter {
  def MaxCount: Int

  def allFields: List[FieldId]

  def extraFields(c: Coordinates): List[String]

  def orderBy: Option[String] = None

  implicit val shapeInterpreter: ShapeInterpreter
}

object ADQLInterpreter {

  // Find the target closest to the base. Useful for debugging
  def baseOnly(using gaia: CatalogAdapter.Gaia, si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount         = 1
      val shapeInterpreter = si

      def allFields: List[FieldId] = gaia.allFields

      override def extraFields(c: Coordinates) =
        List(
          f"DISTANCE(POINT(${c.ra.toAngle.toDoubleDegrees}%9.8f, ${c.dec.toAngle.toSignedDoubleDegrees}%9.8f), POINT(ra, dec)) AS ang_sep"
        )

      override def orderBy =
        Some("ang_sep DESC")
    }

  // Find one target. Useful for debugging
  def oneTarget(using CatalogAdapter.Gaia, ShapeInterpreter): ADQLInterpreter =
    nTarget(1)

  // Find n targets around the base
  def nTarget(
    count:      Int
  )(using gaia: CatalogAdapter.Gaia, si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount                 = count
      val shapeInterpreter         = si
      def allFields: List[FieldId] = gaia.allFields
      def extraFields(c: Coordinates): List[String] = Nil
    }

  // Find n targets around the base
  def pmCorrected(count: Int, epoch: Epoch)(using
    gaia:                CatalogAdapter.Gaia,
    si:                  ShapeInterpreter
  ): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount                 = count
      val shapeInterpreter         = si
      def allFields: List[FieldId] = gaia.allFields.filter {
        case gaia.raField | gaia.decField | gaia.pmDecField | gaia.pmRaField | gaia.plxField |
            gaia.rvField =>
          false
        case f if f === gaia.epochField => false
        case _                          => true
      }

      def extraFields(c: Coordinates): List[String] = List(
        // Gaia can do pm correction for a given epoch
        // https://www.cosmos.esa.int/web/gaia-users/archive/writing-queries/#epoch_prop_pos
        s"COORD1(EPOCH_PROP_POS(ra, dec, parallax, pmra, pmdec, radial_velocity, ref_epoch, ${epoch.epochYear})) as ra",
        s"COORD2(EPOCH_PROP_POS(ra, dec, parallax, pmra, pmdec, radial_velocity, ref_epoch, ${epoch.epochYear})) as dec",
        s"${epoch.epochYear} as ref_epoch"
      )
    }
}