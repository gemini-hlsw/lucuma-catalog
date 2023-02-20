// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags.arb

import lucuma.ags.GuideStarCandidate
import lucuma.core.math.BrightnessValue
import lucuma.core.math.arb.ArbBrightnessValue
import lucuma.core.model.SiderealTracking
import lucuma.core.model.arb.ArbSiderealTracking
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Cogen.*
import org.scalacheck.*

trait ArbGuideStarCandidate {
  import ArbSiderealTracking.given
  import ArbBrightnessValue.given

  given Arbitrary[GuideStarCandidate] =
    Arbitrary {
      for {
        n <- arbitrary[Long]
        t <- arbitrary[SiderealTracking]
        g <- arbitrary[Option[BrightnessValue]]
      } yield GuideStarCandidate(n, t, g)
    }

  given Cogen[GuideStarCandidate] =
    Cogen[(Long, SiderealTracking, Option[BrightnessValue])].contramap(r =>
      (r.id, r.tracking, r.gBrightness)
    )
}

object ArbGuideStarCandidate extends ArbGuideStarCandidate
