// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.syntax.all._
import fs2.text
import lucuma.core.geom.jts.interpreter._
import lucuma.core.geom.gmos.all.candidatesArea
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.RightAscension
import lucuma.core.model.SiderealTracking
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient
import spire.math.Bounded

import java.time.Instant

trait GaiaQueryPMSample {
  val epoch = Epoch.fromString.getOption("J2022.000").getOrElse(Epoch.J2000)

  implicit val ci =
    ADQLInterpreter.nTarget(10)

  val m81Coords = (RightAscension.fromStringHMS.getOption("16:17:2.410"),
                   Declination.fromStringSignedDMS.getOption("-22:58:33.90")
  ).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)

  val pm = ProperMotion(ProperMotion.RA.milliarcsecondsPerYear.reverseGet(-6060),
                        ProperMotion.Dec.milliarcsecondsPerYear.reverseGet(8298)
  )

  val tracking = SiderealTracking(m81Coords, Epoch.J2000, pm.some, none, none)

  def gaiaQuery[F[_]: Sync](client: Client[F]) = {
    val bc    = BrightnessConstraints(BandsList.GaiaBandsList,
                                   FaintnessConstraint(16),
                                   SaturationConstraint(9).some
    )
    val query = CatalogSearch.gaiaSearchUri(
      TimeRangeQueryByADQL(
        tracking,
        Bounded(Instant.EPOCH, Instant.EPOCH.plusSeconds(365 * 24 * 60 * 60 * 60), 0),
        candidatesArea,
        bc.some
      )
    )

    val request = Request[F](GET, query)
    client
      .stream(request)
      .flatMap(
        _.body
          .through(text.utf8.decode)
          .evalTap(a => Sync[F].delay(println(a)))
          .through(CatalogSearch.guideStars[F](CatalogAdapter.Gaia))
      )
      .compile
      .toList
  }
}

object GaiaQueryPMApp extends IOApp.Simple with GaiaQueryPMSample {
  def run =
    JdkHttpClient
      .simple[IO]
      .use(gaiaQuery[IO])
      .flatMap(x => IO.println(pprint.apply(x)))
}