// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.syntax.all._
import fs2.text
import lucuma.ags._
import lucuma.core.enum.CloudExtinction
import lucuma.core.enum.GuideSpeed
import lucuma.core.enum.ImageQuality
import lucuma.core.enum.SkyBackground
import lucuma.core.geom.gmos.all.candidatesArea
import lucuma.core.geom.jts.interpreter._
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.RightAscension
import lucuma.core.math.Wavelength
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient

trait AgsSelectionSample {
  val epoch = Epoch.fromString.getOption("J2022.000").getOrElse(Epoch.J2000)

  implicit val ci =
    ADQLInterpreter.pmCorrected(1000, epoch)

  val m81Coords = (RightAscension.fromStringHMS.getOption("16:17:2.410"),
                   Declination.fromStringSignedDMS.getOption("-22:58:33.90")
  ).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)

  def gaiaQuery[F[_]: Sync](client: Client[F], bc: BrightnessConstraints) = {
    val query   = CatalogSearch.gaiaSearchUri(QueryByADQL(m81Coords, candidatesArea, bc.some))
    val request = Request[F](GET, query)
    client
      .stream(request)
      .flatMap(
        _.body
          .through(text.utf8.decode)
          .through(CatalogSearch.guideStars[F](CatalogAdapter.Gaia))
      )
      .compile
      .toList
  }
}

object AgsSelectionSampleApp extends IOApp.Simple with AgsSelectionSample {
  def run =
    JdkHttpClient
      .simple[IO]
      .use(
        gaiaQuery[IO](_,
                      gaiaBrightnessConstraints(GuideSpeed.Fast,
                                                Wavelength.fromNanometers(700).get,
                                                SkyBackground.Darkest,
                                                ImageQuality.PointTwo,
                                                CloudExtinction.PointFive
                      )
        )
      )
      .flatMap(x => IO.println(x.length))
}
