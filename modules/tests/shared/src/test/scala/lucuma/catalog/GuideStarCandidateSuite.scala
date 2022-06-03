// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.kernel.laws.discipline._
import lucuma.ags.arb._
import lucuma.core.model.arb._
import lucuma.core.optics.laws.discipline.SplitEpiTests
import munit._

class GuideStarCandidateSuite extends DisciplineSuite {
  import ArbGuideStarCandidate._
  import ArbTarget._

  // Laws
  checkAll("Eq[GuideStarCandidate]", EqTests[GuideStarCandidate].eqv)
  // optics
  checkAll("GuideStarCandidate.siderealTarget",
           SplitEpiTests(GuideStarCandidate.siderealTarget).splitEpi
  )
}
