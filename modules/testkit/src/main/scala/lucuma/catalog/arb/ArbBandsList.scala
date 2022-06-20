// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.arb

import lucuma.catalog.BandsList
import lucuma.core.enums.Band
import lucuma.core.util.arb.ArbEnumerated._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck._

trait ArbBandsList {

  implicit val arbBandsList: Arbitrary[BandsList] =
    Arbitrary {
      for {
        b <- arbitrary[Band]
      } yield BandsList.SingleBand(b)
    }

  implicit val cogBandsList: Cogen[BandsList] =
    Cogen[List[Band]].contramap(_.bands)
}

object ArbBandsList extends ArbBandsList
