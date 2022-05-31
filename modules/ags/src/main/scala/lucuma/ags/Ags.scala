// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.syntax.all._
import lucuma.core.enum.PortDisposition
import lucuma.core.enum.GuideSpeed
import lucuma.core.enum.GmosNorthFpu
import lucuma.core.enum.GmosSouthFpu
import lucuma.core.geom.ShapeExpression
import lucuma.core.geom.syntax.all._
import lucuma.core.math.Angle
import lucuma.core.math.Offset
import lucuma.core.math.Coordinates
import lucuma.core.model.ConstraintSet
import fs2._
import lucuma.core.geom.gmos.probeArm
import lucuma.core.geom.gmos.scienceArea
import lucuma.core.enum.Band
import lucuma.core.enum.ImageQuality
import lucuma.core.geom.Area
import lucuma.core.model.Target
import lucuma.core.geom.jts.interpreter._
import lucuma.core.math.Wavelength

final case class AGSPosition(posAngle: Angle, offsetPos: Offset)

sealed trait AGSParams {
  def probe: GuideProbe
  def isReachable(gsOffset:    Offset, position:      AGSPosition): Boolean
  def vignettingArea(position: AGSPosition)(gsOffset: Offset): ShapeExpression
}

final case class GmosAGSParams(
  fpu:  Option[Either[GmosNorthFpu, GmosSouthFpu]],
  port: PortDisposition
) extends AGSParams {
  val probe = GuideProbe.OIWFS

  def isReachable(gsOffset: Offset, position: AGSPosition): Boolean =
    patrolField(position).eval.contains(gsOffset)

  def patrolField(position: AGSPosition): ShapeExpression =
    probeArm.patrolFieldAt(position.posAngle, position.offsetPos, fpu, port)

  override def vignettingArea(position: AGSPosition)(gsOffset: Offset): ShapeExpression =
    scienceArea.shapeAt(position.posAngle, position.offsetPos, fpu) ∩
      probeArm.shapeAt(position.posAngle, gsOffset, position.offsetPos, fpu, port)
}

object AGS {

  def runAnalysis(
    conditions:      ConstraintSet,
    sequenceOffsets: Map[AGSPosition, AGSParams],
    wavelength:      Wavelength,
    gsOffset:        Offset,
    gsc:             GuideStarCandidate
  ): Map[AGSPosition, AgsAnalysis] =
    sequenceOffsets.map { case (pos, params) =>
      val analysis =
        if (!params.isReachable(gsOffset, pos))
          AgsAnalysis.NotReachable(pos, params.probe, gsc)
        else
          magnitudeAnalysis(
            conditions,
            params.probe,
            gsOffset,
            gsc,
            wavelength,
            // calculate vignetting
            params.vignettingArea(pos)(_).eval.area
          )
      pos -> analysis
    }

  /**
   * Analysis of the suitability of the magnitude of the given guide star regardless of its
   * reachability.
   */
  def magnitudeAnalysis(
    constraints:    ConstraintSet,
    guideProbe:     GuideProbe,
    gsOffset:       Offset,
    guideStar:      GuideStarCandidate,
    wavelength:     Wavelength,
    vignettingArea: Offset => Area
  ): AgsAnalysis = {
    import AgsGuideQuality._
    import AgsAnalysis._

    // Called when we know that a valid guide speed can be chosen for the given guide star.
    // Determine the quality and return an analysis indicating that the star is usable.
    def usable(guideSpeed: GuideSpeed): AgsAnalysis = {
      def worseOrEqual(iq: ImageQuality) = constraints.imageQuality >= iq

      val quality = guideSpeed match {
        case GuideSpeed.Fast   =>
          DeliversRequestedIq
        case GuideSpeed.Medium =>
          // if (worseOrEqual(ImageQuality.PERCENT_70)) DeliversRequestedIq
          if (worseOrEqual(ImageQuality.PointSix)) DeliversRequestedIq
          else PossibleIqDegradation
        case GuideSpeed.Slow   =>
          // if (worseOrEqual(ImageQuality.PERCENT_85)) DeliversRequestedIq
          if (worseOrEqual(ImageQuality.PointEight)) DeliversRequestedIq
          // else if (worseOrEqual(ImageQuality.PERCENT_70)) PossibleIqDegradation
          else if (worseOrEqual(ImageQuality.PointSix)) PossibleIqDegradation
          else IqDegradation
      }

      Usable(guideProbe, guideStar, guideSpeed.some, quality, vignettingArea(gsOffset))
    }

    // Do we have a g magnitude
    guideStar.gBrightness
      .map { g =>
        fastestGuideSpeed(constraints, wavelength, g)
          .map { speed =>
            usable(speed)
          }
          .getOrElse(NoGuideStarForProbe(guideProbe))
      }
      .getOrElse(NoMagnitudeForBand(guideProbe, guideStar))
  }

  /**
   * FS2 pipe to convert a stream of Sideral targets to analyzed guidestars
   */
  def agsAnalysis[F[_]](
    constraints:     ConstraintSet,
    wavelength:      Wavelength,
    baseCoordinates: Coordinates,
    sequenceOffsets: Map[AGSPosition, AGSParams]
  ): Pipe[F, Target.Sidereal, (GuideStarCandidate, Map[AGSPosition, AgsAnalysis])] =
    in =>
      in.map { u =>
        val gsc    = GuideStarCandidate.siderealTarget.get(u)
        val offset = baseCoordinates.diff(gsc.tracking.baseCoordinates).offset
        gsc -> runAnalysis(constraints, sequenceOffsets, wavelength, offset, gsc)
      }

  /**
   * Determines the fastest possible guide speed (if any) that may be used for guiding given a star
   * with the indicated magnitude.
   */
  def fastestGuideSpeed(
    constraints: ConstraintSet,
    wavelength:  Wavelength,
    magnitude:   BigDecimal
  ): Option[GuideSpeed] =
    GuideSpeed.all.find { speed => // assumes the values are sorted fast to slow
      gaiaBrightnessConstraints(constraints, speed, wavelength).contains(Band.Gaia, magnitude)
    }

}
