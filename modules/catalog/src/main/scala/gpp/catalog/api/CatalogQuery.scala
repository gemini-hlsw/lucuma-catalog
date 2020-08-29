// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog

import cats._
import cats.implicits._
import lucuma.core.enum.MagnitudeBand
import lucuma.core.model.Target

trait QueryResultsFilter {
  def filter(t: Target): Boolean
}

sealed abstract class CatalogName(val id: String, val displayName: String)
    extends Product
    with Serializable    {

  def supportedBands: List[MagnitudeBand] =
    Nil

  // Indicates what is the band used when a generic R band is required
  def rBand: MagnitudeBand =
    MagnitudeBand.Uc

}

object CatalogName {

  case object SIMBAD extends CatalogName("simbad", "Simbad")

  implicit val catalogNameEquals: Eq[CatalogName] =
    Eq.by(_.id)
}

/**
  * Represents a query on a catalog
  */
sealed trait CatalogQuery {
  val catalog: CatalogName

  def filter(t:       Target): Boolean
  def isSuperSetOf(c: CatalogQuery): Boolean
}

/**
  * Name based query, typically without filtering
  */
case class NameCatalogQuery(search: String, catalog: CatalogName) extends CatalogQuery {
  def filter(t:       Target)       = true
  def isSuperSetOf(c: CatalogQuery) = false
}

object CatalogQuery {

  def nameSearch(search: String): CatalogQuery =
    NameCatalogQuery(search, CatalogName.SIMBAD)

}
