// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.arb

import cats.data.NonEmptyList
import eu.timepit.refined.scalacheck.string._
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog._
import org.scalacheck.Cogen
import org.scalacheck.Gen
import org.scalacheck._

trait ArbUcd {
  val genNonEmptyString = implicitly[Arbitrary[NonEmptyString]].arbitrary

  implicit val arbUcd: Arbitrary[Ucd] =
    Arbitrary {
      for {
        a <- Gen.nonEmptyListOf[NonEmptyString](genNonEmptyString)
      } yield Ucd(NonEmptyList.fromList(a).get)
    }

  implicit val cogenUcd: Cogen[Ucd] =
    Cogen[List[String]].contramap(_.tokens.map(_.value).toList)

}

object ArbUcd extends ArbUcd
