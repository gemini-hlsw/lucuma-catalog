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
import lucuma.core.geom.jts.interpreter.given
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

trait AgsSelectionSample {

  given gaia: CatalogAdapter.Gaia = CatalogAdapter.Gaia3Lite

  given ADQLInterpreter =
    ADQLInterpreter.nTarget(30000)

  val coords = (RightAscension.fromStringHMS.getOption("15:28:00.668"),
                Declination.fromStringSignedDMS.getOption("+64:45:47.40")
  ).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)

  def gaiaQuery[F[_]: Sync](client: Client[F]): Stream[F, Target.Sidereal] = {
    val query   =
      CatalogSearch.gaiaSearchUri(QueryByADQL(coords, candidatesArea, widestConstraints.some))
    val request = Request[F](GET, query)
    client
      .stream(request)
      .flatMap(
        _.body
          .through(text.utf8.decode)
          // .evalTap(a => Sync[F].delay(println(a)))
          .through(CatalogSearch.guideStars[F](gaia))
          .collect { case Right(t) => t }
          // .evalTap(a => Sync[F].delay(println(a)))
      )
  }
}

object AgsSelectionSampleApp extends IOApp.Simple with AgsSelectionSample {
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
  val positions  = NonEmptyList.of(AgsPosition(Angle.fromDoubleDegrees(1), Offset.Zero),
                                  AgsPosition(Angle.fromDoubleDegrees(1).flip, Offset.Zero)
  )

  def run =
    JdkHttpClient
      .simple[IO]
      .flatMap(
        gaiaQuery[IO](_)
          .map(GuideStarCandidate.siderealTarget.get)
          .compile
          .toList
          .map(candidates =>
            Ags
              .agsAnalysis(
                constraints,
                wavelength,
                coords,
                List(coords),
                positions,
                AgsParams.GmosAgsParams(
                  GmosNorthFpu.LongSlit_1_00.asLeft.some,
                  PortDisposition.Side
                ),
                candidates
              )
              .sortPositions(positions)
          )
      )
      .flatTap(x => IO.println(x.length))
      // .flatMap(x => x.filter(_.isUsable).traverse(u => IO(pprint.pprintln(u))))
      .void
}
