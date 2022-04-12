// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect.Sync
import cats.effect.IO
import cats.effect.IOApp
import cats.syntax.all._
import fs2.text
import lucuma.core.geom.ShapeExpression
import lucuma.core.geom.jts.interpreter._
import lucuma.core.geom.syntax.all._
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.RightAscension
import lucuma.core.math.syntax.int._
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.jdkhttpclient.JdkHttpClient

trait GaiaQuerySample {
  def gmosAgsField =
    // 4.9 arcmin radius
    ShapeExpression.centeredEllipse((4.9 * 60 * 1000 * 2).toInt.mas,
                                    (4.9 * 60 * 1000 * 2).toInt.mas
    )

  implicit val ci = ADQLInterpreter.pmCorrected(1)

  val m81Coords = (RightAscension.fromStringHMS.getOption("16:17:2.410"),
                   Declination.fromStringSignedDMS.getOption("-22:58:33.90")
  ).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)

  def gaiaQuery[F[_]: Sync](client: Client[F]) = {
    val query   = CatalogSearch.gaiaSearchUri(QueryByADQL(m81Coords, gmosAgsField))
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

object GaiaQueryApp extends IOApp.Simple with GaiaQuerySample {
  def run =
    JdkHttpClient
      .simple[IO]
      .use(gaiaQuery[IO])
      .flatMap(x => IO.println(pprint.apply(x)))
}
