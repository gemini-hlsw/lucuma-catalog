// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.votable.arb

import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog.votable.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Cogen
import org.scalacheck._

trait ArbFieldId {
  import ArbUcd.given

  given Arbitrary[FieldId] =
    Arbitrary {
      for {
        i <- arbitrary[NonEmptyString]
        u <- arbitrary[Option[Ucd]]
      } yield FieldId(i, u)
    }

  given Cogen[FieldId] =
    Cogen[(String, Option[Ucd])].contramap(x => (x.id.value, x.ucd))

}

object ArbFieldId extends ArbFieldId