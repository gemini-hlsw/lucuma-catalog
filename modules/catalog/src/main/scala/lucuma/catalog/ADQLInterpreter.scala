// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import lucuma.core.geom.ShapeInterpreter
import lucuma.core.math.Coordinates

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
  val gaia = CatalogAdapter.Gaia

  val allBaseFields = gaia.allFields

  // Find the target closest to the base. Useful for debugging
  def baseOnly(implicit si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount         = 1
      val shapeInterpreter = si

      def allFields: List[FieldId] = allBaseFields

      override def extraFields(c: Coordinates) =
        List(
          f"DISTANCE(POINT(${c.ra.toAngle.toDoubleDegrees}%9.8f, ${c.dec.toAngle.toSignedDoubleDegrees}%9.8f), POINT(ra, dec)) AS ang_sep"
        )

      override def orderBy =
        Some("ang_sep DESC")
    }

  // Find one target. Useful for debugging
  def oneTarget(implicit si: ShapeInterpreter): ADQLInterpreter =
    nTarget(1)

  // Find n targets around the base
  def nTarget(count: Int)(implicit si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount                                  = count
      val shapeInterpreter                          = si
      def allFields: List[FieldId]                  = allBaseFields
      def extraFields(c: Coordinates): List[String] = Nil
    }

  // Find n targets around the base
  def pmCorrected(count: Int)(implicit si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount                 = count
      val shapeInterpreter         = si
      def allFields: List[FieldId] = allBaseFields.filter {
        case gaia.raField | gaia.decField | gaia.pmDecField | gaia.pmRaField | gaia.plxField |
            gaia.rvField =>
          false
        case _ => true
      }

      def extraFields(c: Coordinates): List[String] = List( // "array_element(pm,1) as ra_prop",
        "COORD1(EPOCH_PROP_POS(ra, dec, parallax, pmra, pmdec, radial_velocity, 2015.5, 2000)) as ra",
        "COORD2(EPOCH_PROP_POS(ra, dec, parallax, pmra, pmdec, radial_velocity, 2015.5, 2000)) as dec"
      )
    }
}
