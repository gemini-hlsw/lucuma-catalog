// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.arb

import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog._
import org.scalacheck.Arbitrary._
import org.scalacheck.Cogen
import org.scalacheck._
import eu.timepit.refined.scalacheck.string._

trait ArbFieldId {
  import ArbUcd._

  implicit val arbFieldId: Arbitrary[FieldId] =
    Arbitrary {
      for {
        i <- arbitrary[NonEmptyString]
        u <- arbitrary[Ucd]
      } yield FieldId(i, u)
    }

  implicit val cogenFieldId: Cogen[FieldId] =
    Cogen[(String, Ucd)].contramap(x => (x.id.value, x.ucd))

}

object ArbFieldId extends ArbFieldId
