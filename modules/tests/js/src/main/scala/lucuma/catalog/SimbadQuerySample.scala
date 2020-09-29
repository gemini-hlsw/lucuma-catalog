// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.implicits._
import cats.effect._
import sttp.client3._
import fs2._
import lucuma.core.enum.CatalogName
import scala.scalajs.js
import scala.scalajs.js.annotation._
import cats.data.Validated

@JSExportTopLevel("main")
object SimbadQuerySample extends IOApp {

  @js.native
  @JSImport("node-fetch", JSImport.Namespace)
  val nodeFetch: js.Object = js.native

  @js.native
  @JSImport("node-fetch", "Request")
  val Request: js.Object         = js.native
  @js.native
  @JSImport("abortcontroller-polyfill/dist/cjs-ponyfill", "AbortController")
  def AbortController: js.Object = js.native

  @js.native
  @JSImport("fetch-headers", JSImport.Namespace)
  val fetchHeaders: js.Object = js.native

  def run(args: List[String]): IO[ExitCode] = {
    // This is required to import these modules in node
    // It shouldn't be necessary in a browser
    val g = scalajs.js.Dynamic.global.globalThis
    g.fetch = nodeFetch
    g.AbortController = AbortController
    g.Headers = fetchHeaders
    g.Request = Request

    val backend  = FetchBackend()
    def response = basicRequest
      .post(
        uri"http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2SLAQ%20J000008.13%2B001634.6&output.format=VOTable"
      )
      .send(backend)

    IO.fromFuture(IO(response))
      .flatMap {
        _.body
          .traverse(
            Stream
              .emit[IO, String](_)
              .through(VoTableParser.targets(CatalogName.Simbad))
              .compile
              .lastOrError
          )
          .flatMap {
            case Right(Validated.Valid(t)) => IO(pprint.pprintln(t))
            case e                         => IO(pprint.pprintln(e))
          }
      }
      .as(ExitCode.Success)
  }
}
