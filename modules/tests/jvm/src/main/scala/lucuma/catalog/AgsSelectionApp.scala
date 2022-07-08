// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.syntax.all._
import fs2._
import fs2.text
import lucuma.ags._
import lucuma.core.enums.CloudExtinction
import lucuma.core.enums.GmosNorthFpu
import lucuma.core.enums.ImageQuality
import lucuma.core.enums.PortDisposition
import lucuma.core.enums.SkyBackground
import lucuma.core.enums.WaterVapor
import lucuma.core.geom.gmos.all.candidatesArea
import lucuma.core.geom.jts.interpreter._
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Offset
import lucuma.core.math.RightAscension
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.model.ElevationRange
import lucuma.core.model.SiderealTracking
import lucuma.core.model.Target
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient

import java.time.Instant

trait AgsSelectionSample {

  implicit val gaia: CatalogAdapter.Gaia = CatalogAdapter.Gaia3Lite

  implicit val ci: ADQLInterpreter =
    ADQLInterpreter.nTarget(30000)

  val coords = (RightAscension.fromStringHMS.getOption("18:15:47.550"),
                Declination.fromStringSignedDMS.getOption("-29:49:05.00")
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
  val constraints = ConstraintSet(ImageQuality.PointTwo,
                                  CloudExtinction.PointFive,
                                  SkyBackground.Dark,
                                  WaterVapor.Wet,
                                  ElevationRange.AirMass.Default
  )

  val wavelength = Wavelength.fromNanometers(300).get

  def run =
    JdkHttpClient
      .simple[IO]
      .use(
        gaiaQuery[IO](_)
          .map(GuideStarCandidate.siderealTarget.get)
          .through(
            Ags.agsAnalysisStream[IO](
              constraints,
              wavelength,
              SiderealTracking.const(coords),
              AgsPosition(Angle.Angle0, Offset.Zero),
              AgsParams.GmosAgsParams(
                GmosNorthFpu.LongSlit_5_00.asLeft.some,
                PortDisposition.Bottom
              ),
              Instant.now()
            )
          )
          .compile
          .toList
      )
      .flatTap(x => IO.println(x.length))
      // .flatMap(x => x.traverse(u => IO(pprint.pprintln(u))))
      .void
}
