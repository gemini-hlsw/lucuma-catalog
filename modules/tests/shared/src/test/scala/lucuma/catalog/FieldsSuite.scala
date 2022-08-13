// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.kernel.laws.discipline.EqTests
import lucuma.catalog.arb.all._

class FieldsSuite extends munit.DisciplineSuite {
  checkAll("Eq[FieldId]", EqTests[FieldId].eqv)
}
