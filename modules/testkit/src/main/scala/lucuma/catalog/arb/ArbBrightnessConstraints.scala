// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.arb

import lucuma.catalog.BandsList
import lucuma.catalog.BrightnessConstraints
import lucuma.catalog.FaintnessConstraint
import lucuma.catalog.SaturationConstraint
import lucuma.core.util.arb.ArbEnumerated._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck._

trait ArbBrightnessConstraints {

  implicit val arbFaintnessConstraints: Arbitrary[FaintnessConstraint] =
    Arbitrary {
      for {
        b <- arbitrary[BigDecimal]
      } yield FaintnessConstraint(b)
    }

  implicit val cogFaintnessConstraints: Cogen[FaintnessConstraint] =
    Cogen[BigDecimal].contramap(_.brightness)

  implicit val arbSaturationConstraints: Arbitrary[SaturationConstraint] =
    Arbitrary {
      for {
        b <- arbitrary[BigDecimal]
      } yield SaturationConstraint(b)
    }

  implicit val cogSaturationConstraints: Cogen[SaturationConstraint] =
    Cogen[BigDecimal].contramap(_.brightness)

  implicit val arbBrightnessConstraints: Arbitrary[BrightnessConstraints] =
    Arbitrary {
      for {
        f <- arbitrary[FaintnessConstraint]
        l <- arbitrary[Option[SaturationConstraint]]
      } yield BrightnessConstraints(BandsList.GaiaBandsList, f, l)
    }

  implicit val cogBrightnessConstraints: Cogen[BrightnessConstraints] =
    Cogen[(FaintnessConstraint, Option[SaturationConstraint])].contramap(x =>
      (x.faintnessConstraint, x.saturationConstraint)
    )
}

object ArbBrightnessConstraints extends ArbBrightnessConstraints
