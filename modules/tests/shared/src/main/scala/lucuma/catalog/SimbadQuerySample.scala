// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.data.ValidatedNec
import cats.effect.Concurrent
import fs2._
import lucuma.core.enum.CatalogName
import org.http4s.Method._
import org.http4s.Request
import org.http4s.client.Client
import org.http4s.syntax.all._

trait SimbadQuerySample {

  def simbadQueryUri(id: String) =
    uri"http://simbad.u-strasbg.fr/simbad/sim-id"
      .withQueryParam("output.format", "VOTable")
      .withQueryParam("Ident", id)

  def simbadQuery[F[_]: Concurrent](
    client: Client[F]
  ): F[List[ValidatedNec[CatalogProblem, CatalogTargetResult]]] = {
    val request = Request[F](GET, simbadQueryUri("Vega"))
    client
      .stream(request)
      .flatMap(
        _.body
          .through(text.utf8.decode)
          .through(VoTableParser.targets[F](CatalogName.Simbad))
      )
      .compile
      .toList
  }
}
