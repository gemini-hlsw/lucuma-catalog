// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags.arb

import lucuma.ags.GuideStarCandidate
import lucuma.core.model.SiderealTracking
import lucuma.core.model.arb.ArbSiderealTracking
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Cogen._
import org.scalacheck._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.scalacheck.string._

trait ArbGuideStarCandidate {
  import ArbSiderealTracking._

  implicit val arbGuideStarCandidate: Arbitrary[GuideStarCandidate] =
    Arbitrary {
      for {
        n <- arbitrary[NonEmptyString]
        t <- arbitrary[SiderealTracking]
        g <- arbitrary[Option[BigDecimal]]
      } yield GuideStarCandidate(n, t, g)
    }

  implicit val cogGuideStarCandidate: Cogen[GuideStarCandidate] =
    Cogen[(String, SiderealTracking, Option[BigDecimal])].contramap(r =>
      (r.name.value, r.tracking, r.gBrightness)
    )
}

object ArbGuideStarCandidate extends ArbGuideStarCandidate
