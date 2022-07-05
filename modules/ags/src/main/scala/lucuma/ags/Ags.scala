// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.syntax.all._
import fs2._
import lucuma.catalog.BrightnessConstraints
import lucuma.core.enums.Band
import lucuma.core.enums.GuideSpeed
import lucuma.core.enums.ImageQuality
import lucuma.core.geom.Area
import lucuma.core.math.Coordinates
import lucuma.core.math.Offset
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet

object Ags {

  def runAnalysis(
    conditions: ConstraintSet,
    gsOffset:   Offset,
    pos:        AgsPosition,
    params:     AgsParams,
    gsc:        GuideStarCandidate
  )(
    speeds:     List[(GuideSpeed, BrightnessConstraints)],
    calcs:      Map[AgsPosition, AgsGeomCalc]
  ): AgsAnalysis = {
    val geoms = calcs.get(pos)
    if (!geoms.exists(_.isReachable(gsOffset)))
      AgsAnalysis.NotReachable(pos, params.probe, gsc)
    else
      magnitudeAnalysis(
        conditions,
        params.probe,
        gsOffset,
        gsc,
        // calculate vignetting
        geoms
          .map(c => (o: Offset) => c.vignettingArea(o))
          .getOrElse((_: Offset) => Area.MaxArea)
      )(speeds)
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
    vignettingArea: Offset => Area
  )(speeds:         List[(GuideSpeed, BrightnessConstraints)]): AgsAnalysis = {
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
          // TODO Review this limit
          if (worseOrEqual(ImageQuality.PointSix)) DeliversRequestedIq
          else PossibleIqDegradation
        case GuideSpeed.Slow   =>
          // TODO Review this limit
          if (worseOrEqual(ImageQuality.PointEight)) DeliversRequestedIq
          // TODO Review this limit
          else if (worseOrEqual(ImageQuality.PointSix)) PossibleIqDegradation
          else IqDegradation
      }

      Usable(guideProbe, guideStar, guideSpeed.some, quality, vignettingArea(gsOffset))
    }

    // Do we have a g magnitude
    guideStar.gBrightness
      .map { g =>
        speeds
          .find(_._2.contains(Band.Gaia, g))
          .map(u => usable(u._1))
          .getOrElse(NoGuideStarForProbe(guideProbe, guideStar))
      }
      .getOrElse(NoMagnitudeForBand(guideProbe, guideStar))

  }

  /**
   * FS2 pipe to do analysis of a stream of Candidate Guide Stars
   */
  def agsAnalysisStream[F[_]](
    constraints:     ConstraintSet,
    wavelength:      Wavelength,
    baseCoordinates: Coordinates,
    position:        AgsPosition,
    params:          AgsParams
  ): Pipe[F, GuideStarCandidate, AgsAnalysis] = {

    // Cache the limits for different speeds
    val guideSpeeds = guideSpeedLimits(constraints, wavelength)
    // This is essentially a cache of geometries avoiding calculatting them
    // over and over again as they don't change for different positions
    val calcs       = params.posCalculations(List(position))

    in =>
      in.map { gsc =>
        val offset = baseCoordinates.diff(gsc.tracking.baseCoordinates).offset
        runAnalysis(constraints, offset, position, params, gsc)(guideSpeeds, calcs)
      }
  }

  /**
   * Do analysis of a list of Candidate Guide Stars
   */
  def agsAnalysis(
    constraints:     ConstraintSet,
    wavelength:      Wavelength,
    baseCoordinates: Coordinates,
    position:        AgsPosition,
    params:          AgsParams,
    candidates:      List[GuideStarCandidate]
  ): List[AgsAnalysis] = {
    // Cache the limits for different speeds
    val guideSpeeds = guideSpeedLimits(constraints, wavelength)
    // This is essentially a cache of geometries avoiding calculatting them
    // over and over again as they don't change for different positions
    val calcs       = params.posCalculations(List(position))

    candidates.map { gsc =>
      val offset = baseCoordinates.diff(gsc.tracking.baseCoordinates).offset
      runAnalysis(constraints, offset, position, params, gsc)(guideSpeeds, calcs)
    }
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

  /**
   * Calculates brightness limits for each guide speed
   */
  def guideSpeedLimits(
    constraints: ConstraintSet,
    wavelength:  Wavelength
  ): List[(GuideSpeed, BrightnessConstraints)] =
    GuideSpeed.all.map { speed => // assumes the values are sorted fast to slow
      (speed, gaiaBrightnessConstraints(constraints, speed, wavelength))
    }

}
