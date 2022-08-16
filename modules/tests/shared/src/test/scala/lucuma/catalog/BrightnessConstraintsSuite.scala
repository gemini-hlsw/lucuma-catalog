// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.kernel.laws.discipline._
import lucuma.catalog.arb.all.given
import lucuma.core.enums.Band
import munit._

class BrightnessConstraintsSuite extends DisciplineSuite {

  // Laws
  checkAll("Eq[BrightnessConstraints]", EqTests[BrightnessConstraints].eqv)
  checkAll("Order[FaintnessConstraint]", OrderTests[FaintnessConstraint].order)
  checkAll("Order[SaturationConstraint]", OrderTests[SaturationConstraint].order)

  test("filter targets on band and faintness") {
    val bc = BrightnessConstraints(BandsList.GaiaBandsList, FaintnessConstraint(10.0), None)
    assert(bc.contains(Band.Gaia, 3.0))
    // no matching band
    assert(!bc.contains(Band.R, 3.0))
    // Too faint
    assert(!bc.contains(Band.Gaia, 12.0))
  }

  test("filter targets on band, faintness and saturation") {
    val bc = BrightnessConstraints(BandsList.GaiaBandsList,
                                   FaintnessConstraint(10.0),
                                   Some(SaturationConstraint(2))
    )
    assert(bc.contains(Band.Gaia, 3.0))
    // no matching band
    assert(!bc.contains(Band.R, 3.0))
    // Too faint
    assert(!bc.contains(Band.Gaia, 12.0))
    // Saturated
    assert(!bc.contains(Band.Gaia, 1.0))
  }
}
