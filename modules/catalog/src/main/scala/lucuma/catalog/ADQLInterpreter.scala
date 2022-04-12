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

  def extraFields(c: Coordinates): List[String]

  def orderBy: Option[String] = None

  implicit val shapeInterpreter: ShapeInterpreter
}

object ADQLInterpreter {
  // Find the target closest to the base. Useful for debugging
  def baseOnly(implicit si: ShapeInterpreter): ADQLInterpreter =
    new ADQLInterpreter {
      val MaxCount         = 1
      val shapeInterpreter = si

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
      def extraFields(c: Coordinates): List[String] = Nil
    }
}
