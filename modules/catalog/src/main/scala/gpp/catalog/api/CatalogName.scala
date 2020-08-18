// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog.api

import gem.util.Enumerated

// UI model
sealed trait CatalogName extends Product with Serializable

object CatalogName {
  case object Simbad extends CatalogName

  /** @group Typeclass Instances */
  implicit val SectionVisibilityStateEnumerated: Enumerated[CatalogName] =
    Enumerated.of(Simbad)

}
