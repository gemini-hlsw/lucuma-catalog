// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.arb

import lucuma.catalog._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import org.scalacheck.Cogen

trait ArbUcd {

  implicit val arbUcdWord: Arbitrary[UcdWord] =
    Arbitrary(arbitrary[String].map(UcdWord(_)))

  implicit val arbUcd: Arbitrary[Ucd] =
    Arbitrary(arbitrary[List[UcdWord]].map(Ucd(_)))

  implicit val cogenUcdWord: Cogen[UcdWord] =
    Cogen[String].contramap(_.token)

  implicit val cogenUcd: Cogen[Ucd] =
    Cogen[List[UcdWord]].contramap(_.tokens)

}

object ArbUcd extends ArbUcd
