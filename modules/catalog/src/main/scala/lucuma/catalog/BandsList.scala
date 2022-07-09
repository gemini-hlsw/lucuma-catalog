// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.Eq
import lucuma.core.enums.Band

/**
 * Defines a list of bands It is used, e.g. to extract a magnitude from a target
 */
sealed trait BandsList {
  def bands: List[Band]

  /**
   * union operation
   */
  def ∪(that: BandsList): BandsList

}

object BandsList {

  implicit val eqBandsList: Eq[BandsList] = Eq.by(_.bands)

  /**
   * Extracts the first valid Gaia Band Magnitude if available
   */
  case object GaiaBandsList extends BandsList {
    val bands = List(Band.GaiaRP, Band.Gaia) // Order is important

    def ∪(that: BandsList): BandsList = this
  }

}
