// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.syntax.all._
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.core.enum.PortDisposition
import lucuma.core.enum.GuideSpeed
import lucuma.core.enum.GmosNorthFpu
import lucuma.core.enum.GmosSouthFpu
import lucuma.core.geom.ShapeExpression
import lucuma.core.geom.ShapeInterpreter
import lucuma.core.geom.syntax.all._
import lucuma.core.math.Angle
import lucuma.core.math.Offset
import lucuma.core.math.Coordinates
import lucuma.core.model.ConstraintSet
import fs2._
import lucuma.core.geom.gmos.probeArm
import lucuma.core.geom.gmos.scienceArea
import lucuma.core.enum.Band
import spire.syntax.additiveGroup._
import lucuma.core.enum.ImageQuality
import lucuma.core.geom.Area
import scala.collection.immutable.SortedMap
import lucuma.catalog.BandsList
import lucuma.core.model.Target
import lucuma.core.geom.jts.interpreter._

final case class AGSPosition(posAngle: Angle, offsetPos: Offset)

sealed trait AGSParams {
  def probe: GuideProbe
  def isReachable(gsOffset: Offset, position: AGSPosition): Boolean
}

final case class GmosAGSParams(
  conditions: ConstraintSet,
  fpu:        Option[Either[GmosNorthFpu, GmosSouthFpu]],
  port:       PortDisposition
) extends AGSParams {
  val probe = GuideProbe.OIWFS

  def isReachable(gsOffset: Offset, position: AGSPosition): Boolean =
    patrolField(position).eval.contains(gsOffset)

  def patrolField(position: AGSPosition): ShapeExpression =
    probeArm.patrolFieldAt(position.posAngle, position.offsetPos, fpu, port)

  // def scienceArea(posAngle: Angle, offsetPos: Offset): ShapeExpression =
  //   scienceArea.imaging ↗ offsetPos ⟲ posAngle
  //
  // def vignettingArea(posAngle: Angle, offsetPos: Offset)(gs: GuideStarCandidate): ShapeExpression =
  //   scienceArea(posAngle, offsetPos) ∩
  //     GmosOiwfsProbeArm.shapeAt(posAngle, gs.offset, offsetPos, fpu, port)
  //
  // def vignettingArea(
  //   scienceArea: ShapeExpression,
  //   posAngle:    Angle,
  //   offsetPos:   Offset,
  //   gs:          GuideStarCandidate
  // ): ShapeExpression =
  //   scienceArea ∩ GmosOiwfsProbeArm.shapeAt(posAngle, gs.offset, offsetPos, fpu, port)
}

object AGS {
  // implicit class GuideProbeOps(val probe: GuideProbe) extends AnyVal {
  //   def bands: List[Band] = BandsList.GaiaBandsList.bands
  // }

  def runAnalysis(
    sequenceOffsets: Map[AGSPosition, AGSParams],
    gsOffset:        Offset,
    gsc:             GuideStarCandidate
  ): List[AgsAnalysis] =
    sequenceOffsets.map { case (pos, params) =>
      if (!params.isReachable(gsOffset, pos))
        AgsAnalysis.NotReachable(pos, params.probe, gsc)
      else
            magnitudeAnalysis(
              params.conditions,
              params.bt,
              params.probe,
              g,
              // calculate vignetting
              g => params.vignettingArea(scienceArea, pos.posAngle, pos.offsetPos, g).eval.area
            )
    }.toList

  /**
   * Analysis of the suitability of the magnitude of the given guide star regardless of its
   * reachability.
   */
  def magnitudeAnalysis(
    conds:          ConstraintSet,
    mt:             BrightnessTable,
    guideProbe:     GuideProbe,
    guideStar:      GuideStarCandidate,
    vignettingArea: GuideStarCandidate => Area
  ): AgsAnalysis = {
    import AgsGuideQuality._
    import AgsAnalysis._

    // Handles the case where the magnitude falls outside of the acceptable ranges for any guide speed.
    // This handles Andy's 0.5 rule where we might possibly be able to guide if the star is only 0.5 too dim, and
    // otherwise returns the appropriate analysis indicating too dim or too bright.
    def outsideLimits(magCalc: BrightnessCalc, mag: BigDecimal): AgsAnalysis = {
      val adj = BigDecimal.fromDouble(0.5)

      val faintnessLimit  =
        magCalc.constraints(conds, GuideSpeed.Slow).map(_.faintnessConstraint.brightness)
      val saturationLimit = magCalc.constraints(conds, GuideSpeed.Fast).map(_.saturationConstraint)
      val saturated       = saturationLimit.exists(_.exists(_.brightness > mag))

      def almostTooFaint: Boolean = !saturated && faintnessLimit.forall(x => mag <= x + adj)
      def tooFaint: Boolean       = faintnessLimit.forall(x => mag > x + adj)

      if (almostTooFaint)
        Usable(guideProbe,
               guideStar,
               GuideSpeed.Slow.some,
               PossiblyUnusable,
               vignettingArea(guideStar)
        )
      else if (tooFaint) MagnitudeTooFaint(guideProbe, guideStar, true)
      else MagnitudeTooBright(guideProbe, guideStar)
    }

    // Called when we know that a valid guide speed can be chosen for the given guide star.
    // Determine the quality and return an analysis indicating that the star is usable.
    def usable(guideSpeed: GuideSpeed): AgsAnalysis = {
      def worseOrEqual(iq: ImageQuality) = conds.imageQuality >= iq

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

      Usable(guideProbe, guideStar, guideSpeed.some, quality, vignettingArea(guideStar))
    }

    // Find the first band in the guide star that is on the list of possible bands
    def usableMagnitude: Option[(Band, BigDecimal)] =
      guideStar.brightnessIn(guideProbe.bands)

  //   (for {
  //     mc <- mt.calc(conds, guideProbe)
  //     mag = usableMagnitude
  //   } yield {
  //     val analysisOpt =
  //       mag.map(m => fastestGuideSpeed(mc, m._1, m._2, conds).fold(outsideLimits(mc, m._2))(usable))
  //     analysisOpt.getOrElse(NoMagnitudeForBand(guideProbe, guideStar))
  //   }).getOrElse(NoGuideStarCandidateForProbe(guideProbe))
  // }
  /**
   * FS2 pipe to convert a stream of Sideral targets to analyzed guidestars
   */
  def agsAnalysis[F[_]: RaiseThrowable](
    baseCoordinates: Coordinates,
    sequenceOffsets: Map[AGSPosition, AGSParams]
  ): Pipe[F, Target.Sidereal, (GuideStarCandidate, List[AgsAnalysis])] =
    in =>
      in.map { u =>
        val gsc    = GuideStarCandidate.siderealTarget.get(u)
        val offset = baseCoordinates.diff(gsc.tracking.baseCoordinates).offset
        gsc -> runAnalysis(sequenceOffsets, offset, gsc)
      }
  //
  // def search[F[_]](
  //   sequenceOffsets: Map[AGSPosition, AGSParams],
  //   allCandidates:   Stream[F, GuideStarCandidate]
  // )(implicit
  //   si:              ShapeInterpreter
  // ): Stream[F, (GuideStarCandidate, AgsAnalysis)] = {
  //   def select(
  //     params:   AGSParams,
  //     pos:      AGSPosition,
  //     allValid: Stream[F, GuideStarCandidate]
  //   ): Stream[F, (GuideStarCandidate, AgsAnalysis)] =
  //     allCandidates.map { (g: GuideStarCandidate) =>
  //       val analysis: AgsAnalysis =
  //         if (!params.isReachable(g))
  //           AgsAnalysis.NotReachable(params.probe, g)
  //         else
  //           AgsAnalysis.NotReachable(params.probe, g)
  //       g -> analysis
  //     }
  // {
  //   val scienceArea = params.scienceArea(pos.posAngle, pos.offsetPos)
  //   allValid
  //     .map { (g: GuideStarCandidate) =>
  //       // Check what targets are in the patrol field
  //       val analysis =
  //         if (!params.patrolField(pos.posAngle, pos.offsetPos).eval.contains(g.offset)) {
  //           AgsAnalysis.NotReachable(params.probe, g)
  //         } else {
  //           magnitudeAnalysis(
  //             params.conditions,
  //             params.bt,
  //             params.probe,
  //             g,
  //             // calculate vignetting
  //             g => params.vignettingArea(scienceArea, pos.posAngle, pos.offsetPos, g).eval.area
  //           )
  //         }
  //       g -> analysis
  //     }
  // }

  //   Stream
  //     .emits(sequenceOffsets.toList)
  //     .flatMap { case (pos, params) =>
  //       select(params, pos, allCandidates)
  //     }
  //   // .fold(_ ::: _)
  //
  // }
  //
  /**
   * Determines the fastest possible guide speed (if any) that may be used for guiding given a star
   * with the indicated magnitude.
   */
  // def fastestGuideSpeed(
  //   mc: BrightnessCalc,
  //   b:  Band,
  //   m:  BigDecimal,
  //   c:  ConstraintSet
  // ): Option[GuideSpeed] =
  //   GuideSpeed.all.find { gs => // assumes the values are sorted fast to slow
  //     mc.constraints(c, gs).contains(b, m)
  //   }

  //
}
