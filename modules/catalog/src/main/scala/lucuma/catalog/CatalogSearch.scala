// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.data._
import fs2._
import fs2.data.xml._
import lucuma.catalog._
import org.http4s.Uri
import org.http4s.syntax.all._

object CatalogSearch {

  /**
   * Takes a name query and builds a uri to query simbad
   */
  def simbadSearchQuery[F[_]](query: QueryByName): Uri = {
    val simbadUri = uri"http://simbad.u-strasbg.fr/simbad/sim-id"
    val base      = query.proxy.fold(simbadUri)(p =>
      Uri
        .fromString(s"${p}/$simbadUri")
        .getOrElse(sys.error("Cannot build gaia url"))
    )

    base
      .withQueryParam("output.format", "VOTable")
      .withQueryParam("Ident", query.id)
  }

  /**
   * Takes a search query and builds a uri to query gaia
   */
  def gaiaSearchUri[F[_]](
    query:       QueryByADQL
  )(implicit ci: ADQLInterpreter): Uri = {
    val esaUri = uri"https://gea.esac.esa.int/tap-server/tap/sync"
    val base   = query.proxy.fold(esaUri)(p =>
      Uri
        .fromString(s"${p}/$esaUri")
        .getOrElse(sys.error("Cannot build gaia url"))
    )
    base
      .withQueryParam("REQUEST", "doQuery")
      .withQueryParam("LANG", "ADQL")
      .withQueryParam("FORMAT", "votable_plain")
      .withQueryParam("QUERY", ADQLGaiaQuery.adql(query))
  }

  /**
   * FS2 pipe to convert a stream of String to targets
   */
  def siderealTargets[F[_]: RaiseThrowable: MonadError[*[_], Throwable]](
    adapter: CatalogAdapter
  ): Pipe[F, String, ValidatedNec[CatalogProblem, CatalogTargetResult]] =
    in =>
      in.flatMap(Stream.emits(_))
        .through(events[F, Char])
        .through(referenceResolver[F]())
        .through(normalize[F])
        .through(VoTableParser.xml2targets[F](adapter))
}