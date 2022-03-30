// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import org.http4s.dom.FetchClientBuilder

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation._

@nowarn
@JSExportTopLevel("main")
object SimbadQueryApp extends IOApp.Simple with SimbadQuerySample {
  @js.native
  @JSImport("node-fetch", JSImport.Namespace)
  val nodeFetch: js.Object = js.native

  @js.native
  @JSImport("node-fetch", "Request")
  val Request: js.Object = js.native

  @js.native
  @JSImport("abortcontroller-polyfill/dist/cjs-ponyfill", "AbortController")
  val AbortController: js.Object = js.native

  @js.native
  @JSImport("fetch-headers", JSImport.Namespace)
  val fetchHeaders: js.Object = js.native

  def run = {

    // This is required to import these modules in node
    // It shouldn't be necessary in a browser
    val g = scalajs.js.Dynamic.global.globalThis
    g.fetch = nodeFetch
    g.AbortController = AbortController
    g.Headers = fetchHeaders
    g.Request = Request

    FetchClientBuilder[IO].resource
      .use(simbadQuery[IO])
      .flatMap(x => IO.println(pprint.apply(x)))
      .void
  }
}
