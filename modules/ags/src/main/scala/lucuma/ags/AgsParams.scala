// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Eq
import cats.syntax.all._
import lucuma.core.enums.GmosNorthFpu
import lucuma.core.enums.GmosSouthFpu
import lucuma.core.enums.PortDisposition
import lucuma.core.geom.Area
import lucuma.core.geom.gmos.probeArm
import lucuma.core.geom.gmos.scienceArea
import lucuma.core.geom.jts.interpreter._
import lucuma.core.geom.syntax.all._
import lucuma.core.math.Angle
import lucuma.core.math.Offset

final case class AgsPosition(posAngle: Angle, offsetPos: Offset)

object AgsPosition {
  implicit val agsPositionEq: Eq[AgsPosition] = Eq.by(x => (x.posAngle, x.offsetPos))
}

sealed trait AgsGeomCalc {
  // Indicates if the given offset is reachable
  def isReachable(gsOffset: Offset): Boolean

  // Calculates the area vignetted at a given offset
  def vignettingArea(gsOffset: Offset): Area
}

sealed trait AgsParams {
  def probe: GuideProbe

  // Builds an AgsGeom object for each position
  // Some of the geometries don't chage with the position and we can cache them
  def posCalculations(positions: List[AgsPosition]): Map[AgsPosition, AgsGeomCalc]
}

object AgsParams {

  final case class GmosAgsParams(
    fpu:  Option[Either[GmosNorthFpu, GmosSouthFpu]],
    port: PortDisposition
  ) extends AgsParams {
    val probe = GuideProbe.OIWFS

    def posCalculations(positions: List[AgsPosition]): Map[AgsPosition, AgsGeomCalc] =
      positions.map { position =>
        position -> new AgsGeomCalc() {
          val patrolField =
            probeArm.patrolFieldAt(position.posAngle, position.offsetPos, fpu, port).eval

          val scienceAreaShape =
            scienceArea.shapeAt(position.posAngle, position.offsetPos, fpu)

          override def isReachable(gsOffset: Offset): Boolean =
            patrolField.contains(gsOffset)

          override def vignettingArea(gsOffset: Offset): Area =
            (scienceAreaShape ∩
              probeArm.shapeAt(position.posAngle,
                               gsOffset,
                               position.offsetPos,
                               fpu,
                               port
              )).eval.area

        }
      }.toMap

    implicit val gmosParamsEq: Eq[GmosAgsParams] = Eq.by(x => (x.fpu, x.port))
  }

  implicit val agsParamsEq: Eq[AgsParams] = Eq.instance {
    case (GmosAgsParams(f1, p1), GmosAgsParams(f2, p2)) => f1 === f2 && p1 === p2
    case _                                              => false
  }
}
