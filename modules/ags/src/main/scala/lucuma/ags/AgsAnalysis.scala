// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Order
import cats.data.NonEmptyList
import cats.syntax.all.*
import lucuma.catalog.BandsList
import lucuma.core.enums.Band
import lucuma.core.enums.GuideSpeed
import lucuma.core.geom.Area

sealed trait AgsAnalysis {
  def quality: AgsGuideQuality = AgsGuideQuality.Unusable
  def isUsable: Boolean        = quality =!= AgsGuideQuality.Unusable
  def position: AgsPosition
  def target: GuideStarCandidate
  def message(withProbe: Boolean): String
}

object AgsAnalysis {

  case class ProperMotionNotAvailable(target: GuideStarCandidate, position: AgsPosition)
      extends AgsAnalysis {
    override def message(withProbe: Boolean): String =
      "Cannot calculate proper motion."
  }

  case class VignettesScience(target: GuideStarCandidate, position: AgsPosition)
      extends AgsAnalysis {
    override def message(withProbe: Boolean): String =
      "The target overlaps with the science target"
  }

  case class NoGuideStarForProbe(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate,
    position:   AgsPosition
  ) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"$guideProbe " else ""
      s"No ${p}guide star selected."
    }
  }

  case class MagnitudeTooFaint(
    guideProbe:     GuideProbe,
    target:         GuideStarCandidate,
    showGuideSpeed: Boolean,
    position:       AgsPosition
  ) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p  = if (withProbe) s"use $guideProbe" else "guide"
      val gs = if (showGuideSpeed) ", even using the slowest guide speed" else ""
      s"Cannot $p with the star in these conditions$gs."
    }
  }

  case class MagnitudeTooBright(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate,
    position:   AgsPosition
  ) extends AgsAnalysis {
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
  ) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"with ${guideProbe} " else ""
      s"The star is not reachable ${p}at $position."
    }
  }

  case class NoMagnitudeForBand(
    guideProbe: GuideProbe,
    target:     GuideStarCandidate,
    position:   AgsPosition
  ) extends AgsAnalysis {
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
    vignettingArea:       Area,
    position:             AgsPosition
  ) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val qualityMessage = quality match {
        case AgsGuideQuality.DeliversRequestedIq => ""
        case _                                   => s"${quality.message} "
      }
      val p              = if (withProbe) s"${guideProbe} " else ""
      val gs             = guideSpeed.fold("Usable")(gs => s"Guide Speed: ${gs.toString()}")
      s"$qualityMessage$p$gs. vignetting: ${vignettingArea.toMicroarcsecondsSquared} µas^2"
      s"$p $quality $gs. vignetting: ${vignettingArea.toMicroarcsecondsSquared} µas^2"
    }
  }

  object Usable {
    val rankingOrder: Order[Usable] =
      Order.by(u => (u.guideSpeed, u.quality, u.vignettingArea, u.target.gBrightness, u.target.id))

    // This order gives preferences to positions given earlier
    private[ags] def rankingOrderAtPositions(positions: Map[AgsPosition, Int]): Order[Usable] =
      Order.by(u =>
        (u.guideSpeed,
         u.quality,
         u.vignettingArea,
         positions.getOrElse(u.position, Int.MaxValue),
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

  private def rankingOrderAtPositions(positions: Map[AgsPosition, Int]): Order[AgsAnalysis] =
    Order.from {
      case (a: Usable, b: Usable) => Usable.rankingOrderAtPositions(positions).compare(a, b)
      case (_: Usable, _)         => Int.MinValue
      case (_, _: Usable)         => Int.MaxValue
      case _                      => Int.MinValue
    }

  extension (results: List[AgsAnalysis])
    /**
     * This method will sort the analysis and remove duplicates for position, keeping only the ones
     * for the winning position
     */
    def selectBestPosition(positions: NonEmptyList[AgsPosition]): List[AgsAnalysis] = {
      val (usable, nonUsable)                   = results.partition(_.isUsable)
      val positionsIndex: Map[AgsPosition, Int] = positions.zipWithIndex.toList.toMap
      val topUsablePosition                     = usable
        .sorted(rankingOrderAtPositions(positionsIndex).toOrdering)
        .headOption
        .map(_.position)
      usable.filter(u => topUsablePosition.exists(_ === u.position)) :::
        nonUsable.filter(u => topUsablePosition.exists(_ === u.position))

    }

}
