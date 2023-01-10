// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.votable

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.syntax.all.*
import fs2.*
import fs2.text
import lucuma.ags.*
import lucuma.core.enums.CloudExtinction
import lucuma.core.enums.GmosNorthFpu
import lucuma.core.enums.GmosSouthFpu
import lucuma.core.enums.ImageQuality
import lucuma.core.enums.PortDisposition
import lucuma.core.enums.SkyBackground
import lucuma.core.enums.WaterVapor
import lucuma.core.geom.gmos.all.candidatesArea
import lucuma.core.geom.jts.interpreter.*
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Offset
import lucuma.core.math.RightAscension
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.model.ElevationRange
import lucuma.core.model.ElevationRange.AirMass
import lucuma.core.model.ElevationRange.AirMass.DecimalValue
import lucuma.core.model.SiderealTracking
import lucuma.core.model.Target
import org.http4s.Method.*
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient

import java.time.Instant

object AgsSelectionSampleStreamApp extends IOApp.Simple with AgsSelectionSample {
  val constraints = ConstraintSet(
    ImageQuality.PointOne,
    CloudExtinction.PointOne,
    SkyBackground.Dark,
    WaterVapor.Wet,
    AirMass.fromDecimalValues.get(DecimalValue.unsafeFrom(BigDecimal(1.0)),
                                  DecimalValue.unsafeFrom(BigDecimal(1.75))
    )
  )

  val wavelength = Wavelength.fromIntNanometers(520).get

  def run =
    JdkHttpClient
      .simple[IO]
      .flatMap(
        gaiaQuery[IO](_)
          .map(GuideStarCandidate.siderealTarget.get)
          .through(
            Ags
              .agsAnalysisStream(
                constraints,
                wavelength,
                coords,
                List(coords),
                NonEmptyList.of(AgsPosition(Angle.fromDoubleDegrees(-120), Offset.Zero),
                                AgsPosition(Angle.fromDoubleDegrees(120), Offset.Zero)
                ),
                AgsParams.GmosAgsParams(
                  GmosNorthFpu.LongSlit_1_00.asLeft.some,
                  PortDisposition.Side
                )
              )
          )
          .compile
          .toList
      )
      .flatTap(x => IO.println(x.length))
      // .flatMap(x => IO.println(x.head))
      .flatMap(x => x.filter(_.isUsable).traverse(u => IO(pprint.pprintln(u))))
      .void
}
