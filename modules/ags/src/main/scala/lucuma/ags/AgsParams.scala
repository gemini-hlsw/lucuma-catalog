// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Eq
import cats.Order
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import cats.derived.*
import lucuma.core.enums.GmosNorthFpu
import lucuma.core.enums.GmosSouthFpu
import lucuma.core.enums.GuideProbe
import lucuma.core.enums.PortDisposition
import lucuma.core.geom.Area
import lucuma.core.geom.ShapeExpression
import lucuma.core.geom.gmos.probeArm
import lucuma.core.geom.gmos.scienceArea
import lucuma.core.geom.jts.interpreter.given
import lucuma.core.geom.syntax.all.*
import lucuma.core.math.Angle
import lucuma.core.math.Offset
import lucuma.core.math.syntax.int.*

private given Order[Angle] = Angle.SignedAngleOrder

case class AgsPosition(posAngle: Angle, offsetPos: Offset) derives Order

sealed trait AgsGeomCalc:
  // Indicates if the given offset is reachable
  def isReachable(gsOffset: Offset): Boolean

  // Calculates the area vignetted at a given offset
  def vignettingArea(gsOffset: Offset): Area

  // Indicates if the given guide star would vignette the science target
  def overlapsScience(gsOffset: Offset): Boolean

sealed trait AgsParams derives Eq:

  def probe: GuideProbe

  // Builds an AgsGeom object for each position
  // Some of the geometries don't chage with the position and we can cache them
  def posCalculations(positions: NonEmptyList[AgsPosition]): NonEmptyMap[AgsPosition, AgsGeomCalc]

object AgsParams:
  val scienceRadius = 20.arcseconds

  case class GmosAgsParams(
    fpu:  Option[Either[GmosNorthFpu, GmosSouthFpu]],
    port: PortDisposition
  ) extends AgsParams derives Eq:
    val probe = GuideProbe.GmosOIWFS

    def posCalculations(
      positions: NonEmptyList[AgsPosition]
    ): NonEmptyMap[AgsPosition, AgsGeomCalc] =
      val result = positions.map { position =>
        position -> new AgsGeomCalc() {
          private val intersectionPatrolField =
            positions
              .map(_.offsetPos)
              .distinct
              // note we use the outer posAngle but the inner offset
              // we want the intersection of offsets at a single PA
              .map(offset => probeArm.patrolFieldAt(position.posAngle, offset, fpu, port))
              .reduce(_ ∩ _)
              .eval

          private val scienceAreaShape =
            scienceArea.shapeAt(position.posAngle, position.offsetPos, fpu)

          private val scienceTargetArea =
            ShapeExpression.centeredEllipse(scienceRadius,
                                            scienceRadius
            ) ↗ position.offsetPos ⟲ position.posAngle

          override def isReachable(gsOffset: Offset): Boolean =
            intersectionPatrolField.contains(gsOffset)

          def overlapsScience(gsOffset: Offset): Boolean =
            // Calculating with area maybe more precise but it is more costly
            (probeArm
              .shapeAt(position.posAngle, gsOffset, position.offsetPos, fpu, port)
              ∩ scienceTargetArea).maxSide.toMicroarcseconds > 5

          override def vignettingArea(gsOffset: Offset): Area =
            (scienceAreaShape ∩
              probeArm.shapeAt(position.posAngle,
                               gsOffset,
                               position.offsetPos,
                               fpu,
                               port
              )).eval.area

        }
      }
      result.toNem
