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
import lucuma.core.enum.CloudExtinction
import lucuma.core.enum.GmosNorthFpu
import lucuma.core.enum.GuideSpeed
import lucuma.core.enum.ImageQuality
import lucuma.core.enum.PortDisposition
import lucuma.core.enum.SkyBackground
import lucuma.core.enum.WaterVapor
import lucuma.core.geom.gmos.all.candidatesArea
import lucuma.core.geom.jts.interpreter._
import lucuma.core.math.Angle
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.Offset
import lucuma.core.math.RightAscension
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.model.ElevationRange
import lucuma.core.model.Target
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient

trait AgsSelectionSample {
  val epoch = Epoch.fromString.getOption("J2022.000").getOrElse(Epoch.J2000)

  implicit val ci =
    ADQLInterpreter.pmCorrected(30000, epoch)

  val coords = (RightAscension.fromStringHMS.getOption("18:15:47.550"),
                Declination.fromStringSignedDMS.getOption("-29:49:05.00")
  ).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)

  def gaiaQuery[F[_]: Sync](
    client: Client[F],
    bc:     BrightnessConstraints
  ): Stream[F, Target.Sidereal] = {
    val query   = CatalogSearch.gaiaSearchUri(QueryByADQL(coords, candidatesArea, bc.some))
    val request = Request[F](GET, query)
    client
      .stream(request)
      .flatMap(
        _.body
          .through(text.utf8.decode)
          .through(CatalogSearch.guideStars[F](CatalogAdapter.Gaia))
          .collect { case Right(t) => t }
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

  val wavelength = Wavelength.fromNanometers(700).get
  def run        =
    JdkHttpClient
      .simple[IO]
      .use(
        gaiaQuery[IO](_, gaiaBrightnessConstraints(constraints, GuideSpeed.Fast, wavelength))
          .map(GuideStarCandidate.siderealTarget.get)
          .through(
            Ags.agsAnalysisStream[IO](
              constraints,
              Wavelength.fromNanometers(700).get,
              coords,
              AgsPosition(Angle.Angle0, Offset.Zero),
              AgsParams.GmosAgsParams(
                GmosNorthFpu.LongSlit_5_00.asLeft.some,
                PortDisposition.Bottom
              )
            )
          )
          .compile
          .toList
      )
      .flatMap(x => x.traverse(u => IO(pprint.pprintln(u))))
      .void
}
