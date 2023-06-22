// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Eq
import cats.Order
import cats.data.NonEmptyList
import cats.derived.*
import cats.syntax.all.*
import eu.timepit.refined.cats.*
import lucuma.catalog.BandsList
import lucuma.core.enums.Band
import lucuma.core.enums.GuideSpeed
import lucuma.core.geom.Area
import lucuma.core.math.Angle
import lucuma.core.math.BrightnessValue

sealed trait AgsAnalysis derives Eq {
  def quality: AgsGuideQuality = AgsGuideQuality.Unusable
  def isUsable: Boolean        = quality =!= AgsGuideQuality.Unusable
  def target: GuideStarCandidate
  def message(withProbe: Boolean): String
}

object AgsAnalysis {

  case class ProperMotionNotAvailable(target: GuideStarCandidate) extends AgsAnalysis derives Eq {
    override def message(withProbe: Boolean): String =
      "Cannot calculate proper motion."
  }

  case class VignettesScience(target: GuideStarCandidate, position: AgsPosition) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String =
      "The target overlaps with the science target"
  }

  case class NoGuideStarForProbe(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate
  ) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"$guideProbe " else ""
      s"No ${p}guide star selected."
    }
  }

  case class MagnitudeTooFaint(
    guideProbe:     GuideProbe,
    target:         GuideStarCandidate,
    showGuideSpeed: Boolean
  ) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String = {
      val p  = if (withProbe) s"use $guideProbe" else "guide"
      val gs = if (showGuideSpeed) ", even using the slowest guide speed" else ""
      s"Cannot $p with the star in these conditions$gs."
    }
  }

  case class MagnitudeTooBright(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate
  ) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"$guideProbe g" else "G"
      s"${p}uide star is too bright to guide."
    }
  }

  case class NotReachableAtPosition(
    position:   AgsPosition,
    guideProbe: GuideProbe,
    guideSpeed: Option[GuideSpeed],
    target:     GuideStarCandidate
  ) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"with ${guideProbe} " else ""
      s"The star is not reachable ${p}at $position."
    }
  }

  case class NoMagnitudeForBand(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate
  ) extends AgsAnalysis
      derives Eq {
    private val probeBands: List[Band]               = BandsList.GaiaBandsList.bands
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"${guideProbe} g" else "G"
      if (probeBands.length == 1) {
        s"${p}uide star ${probeBands.head}-band magnitude is missing. Cannot determine guiding performance."
      } else {
        s"${p}uide star ${probeBands.map(_.shortName).mkString(", ")}-band magnitudes are missing. Cannot determine guiding performance."
      }
    }
    override val quality: AgsGuideQuality            = AgsGuideQuality.PossiblyUnusable
  }

  case class Usable(
    guideProbe:           GuideProbe,
    target:               GuideStarCandidate,
    guideSpeed:           Option[GuideSpeed],
    override val quality: AgsGuideQuality,
    vignetting:           NonEmptyList[(AgsPosition, Area)]
  ) extends AgsAnalysis
      derives Eq {
    override def message(withProbe: Boolean): String = {
      val qualityMessage = quality match {
        case AgsGuideQuality.DeliversRequestedIq => ""
        case _                                   => s"${quality.message} "
      }
      val p              = if (withProbe) s"${guideProbe} " else ""
      val gs             = guideSpeed.fold("Usable")(gs => s"Guide Speed: ${gs.toString()}")
      s"$qualityMessage$p$gs. vignetting: ${vignetting.head._2.toMicroarcsecondsSquared} µas^2"
      s"$p $quality $gs. vignetting: ${vignetting.head._2.toMicroarcsecondsSquared} µas^2"
    }

    def sortedVignetting: Usable = copy(vignetting = vignetting.sortBy(_._2))
  }

  object Usable {
    val rankingOrder: Order[Usable] =
      Order.by(u =>
        (u.guideSpeed,
         u.quality,
         u.vignetting.minimumBy(_._2)._2.toMicroarcsecondsSquared,
         u.target.gBrightness,
         u.target.id
        )
      )

  }

  val rankingOrder: Order[AgsAnalysis] =
    Order.from {
      case (a: Usable, b: Usable) => Usable.rankingOrder.compare(a, b)
      case (_: Usable, _)         => Int.MinValue
      case (_, _: Usable)         => Int.MaxValue
      case _                      => Int.MinValue
    }

  val rankingOrdering: Ordering[AgsAnalysis] = rankingOrder.toOrdering

  extension (analysis: AgsAnalysis)
    def posAngle: Option[Angle] = analysis match
      case AgsAnalysis.Usable(_, _, _, _, v) => Some(v.head._1.posAngle)
      case _                                 => None

  extension (analysis: Option[AgsAnalysis])
    def posAngle: Option[Angle] = analysis.flatMap(_.posAngle)

  extension (results: List[AgsAnalysis])
    /**
     * This method will sort the analysis for quality and internally for positions that give the
     * lowest vignetting.
     */
    def sortPositions(positions: NonEmptyList[AgsPosition]): List[AgsAnalysis] = {
      val (usable, nonUsable)                = results.partition(_.isUsable)
      val usablePerTarget: List[AgsAnalysis] =
        usable
          .groupBy(_.target.id)
          .map { (_, analyses) =>
            analyses
              .collect { case u: Usable =>
                u
              }
              .reduce((a, b) => a.copy(vignetting = a.vignetting.concatNel(b.vignetting)))
              .sortedVignetting
          }
          .toList
      usablePerTarget.sorted(rankingOrdering) ::: nonUsable
    }

}
