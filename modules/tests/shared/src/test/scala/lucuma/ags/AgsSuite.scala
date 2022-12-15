// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.data.NonEmptyList
import cats.syntax.all._
import lucuma.core.enums._
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Offset
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.model.ElevationRange
import lucuma.core.model.SiderealTracking

class AgsSuite extends munit.FunSuite {
  test("discard science target") {
    val constraints = ConstraintSet(ImageQuality.PointTwo,
                                    CloudExtinction.PointFive,
                                    SkyBackground.Dark,
                                    WaterVapor.Wet,
                                    ElevationRange.AirMass.Default
    )

    val wavelength = Wavelength.fromNanometers(300).get
    val gs         = GuideStarCandidate(0L, SiderealTracking.const(Coordinates.Zero), BigDecimal(15).some)

    assertEquals(
      Ags
        .agsAnalysis(
          constraints,
          wavelength,
          Coordinates.Zero,
          List(Coordinates.Zero),
          NonEmptyList.of(AgsPosition(Angle.Angle0, Offset.Zero)),
          AgsParams.GmosAgsParams(GmosNorthFpu.LongSlit_5_00.asLeft.some, PortDisposition.Bottom),
          List(gs)
        )
        .headOption,
      AgsAnalysis.VignettesScience(gs).some
    )

    val gsOffset =
      GuideStarCandidate(
        0L,
        SiderealTracking.const(
          Coordinates.Zero
            .offsetBy(Angle.Angle0, Offset.signedDecimalArcseconds.reverseGet(0.0, 23.0))
            .get
        ),
        BigDecimal(15).some
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
