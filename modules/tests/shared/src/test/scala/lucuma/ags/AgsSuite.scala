// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.data.NonEmptyList
import cats.syntax.all.*
import lucuma.ags.AgsAnalysis.NoMagnitudeForBand
import lucuma.ags.AgsAnalysis.Usable
import lucuma.core.enums.*
import lucuma.core.geom.Area
import lucuma.core.math.Angle
import lucuma.core.math.BrightnessValue
import lucuma.core.math.Coordinates
import lucuma.core.math.Offset
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.model.ElevationRange
import lucuma.core.model.SiderealTracking

class AgsSuite extends munit.FunSuite {
  val gs1 = GuideStarCandidate.unsafeApply(0L,
                                           SiderealTracking.const(Coordinates.Zero),
                                           (Band.Gaia, BrightnessValue.unsafeFrom(16.05)).some
  )

  val gs2 = GuideStarCandidate.unsafeApply(1L,
                                           SiderealTracking.const(Coordinates.Zero),
                                           (Band.Gaia, BrightnessValue.unsafeFrom(11.23)).some
  )

  test("usable comparisons") {
    val u1 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    val u2 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(1).get
      )
    )
    val u3 = Usable(
      GuideProbe.GmosOIWFS,
      gs2,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    val u4 = Usable(
      GuideProbe.GmosOIWFS,
      gs2,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(2).get
      )
    )

    val u12 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0   -> Area.fromMicroarcsecondsSquared.getOption(0).get,
        Angle.Angle180 -> Area.fromMicroarcsecondsSquared.getOption(10).get
      )
    )

    val u22 = Usable(
      GuideProbe.GmosOIWFS,
      gs2,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0   -> Area.fromMicroarcsecondsSquared.getOption(9).get,
        Angle.Angle180 -> Area.fromMicroarcsecondsSquared.getOption(20).get
      )
    )

    // less vignetting wins
    assert(AgsAnalysis.rankingOrder.compare(u1, u2) < 0)
    // same vignetting but brighter wins
    assert(AgsAnalysis.rankingOrder.compare(u3, u1) < 0)
    // vignetting trumps brighteness
    assert(AgsAnalysis.rankingOrder.compare(u1, u4) < 0)
    // we should check the whole list of vignettes
    assert(AgsAnalysis.rankingOrder.compare(u12, u22) < 0)
  }

  test("sort by brightness") {
    val u1 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle180 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    val u2 = Usable(
      GuideProbe.GmosOIWFS,
      gs2,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    assert(AgsAnalysis.rankingOrder.compare(u1, u2) > 0)
  }

  test("sort positions") {
    val u1 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    val u2 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle180 -> Area.fromMicroarcsecondsSquared.getOption(1).get
      )
    )
    assertEquals(1, List(u1, u2).sortUsablePositions.length)
  }

  test("sort unusable positions") {
    val u1 = Usable(
      GuideProbe.GmosOIWFS,
      gs1,
      GuideSpeed.Fast,
      AgsGuideQuality.DeliversRequestedIq,
      NonEmptyList.of(
        Angle.Angle0 -> Area.fromMicroarcsecondsSquared.getOption(0).get
      )
    )
    val u2 = NoMagnitudeForBand(
      GuideProbe.GmosOIWFS,
      gs1
    )
    assertEquals(1, List(u1, u2).sortUsablePositions.length)
  }

  test("discard science target") {
    val constraints = ConstraintSet(ImageQuality.PointTwo,
                                    CloudExtinction.PointFive,
                                    SkyBackground.Dark,
                                    WaterVapor.Wet,
                                    ElevationRange.AirMass.Default
    )

    val wavelength = Wavelength.fromIntNanometers(300).get

    assertEquals(
      Ags
        .agsAnalysis(
          constraints,
          wavelength,
          Coordinates.Zero,
          List(Coordinates.Zero),
          NonEmptyList.of(AgsPosition(Angle.Angle0, Offset.Zero)),
          AgsParams.GmosAgsParams(GmosNorthFpu.LongSlit_5_00.asLeft.some, PortDisposition.Bottom),
          List(gs1)
        )
        .headOption,
      AgsAnalysis.VignettesScience(gs1, AgsPosition(Angle.Angle0, Offset.Zero)).some
    )

    val gsOffset =
      GuideStarCandidate.unsafeApply(
        0L,
        SiderealTracking.const(
          Coordinates.Zero
            .offsetBy(Angle.Angle0, Offset.signedDecimalArcseconds.reverseGet(0.0, 23.0))
            .get
        ),
        (Band.Gaia, BrightnessValue.unsafeFrom(15)).some
      )

    assert(
      Ags
        .agsAnalysis(
          constraints,
          wavelength,
          Coordinates.Zero,
          Nil,
          NonEmptyList.of(AgsPosition(Angle.Angle0, Offset.Zero)),
          AgsParams.GmosAgsParams(GmosNorthFpu.LongSlit_5_00.asLeft.some, PortDisposition.Bottom),
          List(gsOffset)
        )
        .headOption
        .forall(_.isUsable)
    )
  }
}
