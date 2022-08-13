// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.kernel.laws.discipline._
import lucuma.catalog.arb._
import munit._

class CatalogTargetResultSuite extends DisciplineSuite {
  import ArbCatalogTargetResult._

  // Laws
  checkAll("Eq[CatalogTargetResultSuite]", EqTests[CatalogTargetResult].eqv)
}
