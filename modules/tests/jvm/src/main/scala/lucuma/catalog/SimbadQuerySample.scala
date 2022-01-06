// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import cats.implicits._
import fs2.text
import lucuma.core.enum.CatalogName
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object SimbadQuerySample extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    AsyncHttpClientFs2Backend
      .resource[IO]()
      .use { backend =>
        val response = basicRequest
          .post(
            uri"http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2SLAQ%20J000008.13%2B001634.6&output.format=VOTable"
          )
          .response(asStreamUnsafe(Fs2Streams[IO]))
          .send(backend)

        response
          .flatMap(
            _.body
              .traverse(
                _.through(text.utf8.decode)
                  .through(VoTableParser.targets(CatalogName.Simbad))
                  .compile
                  .lastOrError
              )
          )
          .flatMap {
            case Right(t) => IO(pprint.pprintln(t))
            case _        => IO.unit
          }
      }
      .as(ExitCode.Success)
}
