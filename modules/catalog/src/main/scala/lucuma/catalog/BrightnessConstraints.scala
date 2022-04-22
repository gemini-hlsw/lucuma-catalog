// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.Order

/**
 * Constrain a target if a brightness is fainter than a threshold
 */
case class FaintnessConstraint(brightness: BigDecimal)

object FaintnessConstraint {

  /** @group Typeclass Instances */
  implicit val order: Order[FaintnessConstraint] =
    Order.by(_.brightness)
}

/**
 * Constrain a target's if a brightness is brighter than a threshold
 */
case class SaturationConstraint(brightness: BigDecimal)

object SaturationConstraint {

  /** @group Typeclass Instances */
  implicit val order: Order[SaturationConstraint] =
    Order.by(_.brightness)
}

/**
 * Describes constraints for the brightness of a target
 */
case class BrightnessConstraints(
  searchBands:          BandsList,
  faintnessConstraint:  FaintnessConstraint,
  saturationConstraint: Option[SaturationConstraint]
)
