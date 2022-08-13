// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.data._
import cats.kernel.laws.discipline.EqTests
import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.collection._
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog._
import lucuma.catalog.arb.all.given
import lucuma.refined.*
import munit.DisciplineSuite

class UcdSuite extends munit.FunSuite with DisciplineSuite {
  checkAll("Ucd", EqTests[Ucd].eqv)

  test("detect if is a superset") {
    assert(
      Ucd.unsafeFromString("stat.error;phot.mag;em.opt.i").includes("phot.mag".refined)
    )
    assert(Ucd.unsafeFromString("stat.error;phot.mag;em.opt.i").matches("phot.mag".r))
    assert(Ucd.unsafeFromString("stat.error;phot.mag;em.opt.i").matches("em.opt.(\\w)".r))
  }

  test("parse single token ucds") {
    assertEquals(Ucd.parseUcd("meta.code"), Ucd("meta.code".refined[NonEmpty]).rightNec)
  }

  test("parse multi token ucds and preserve order") {
    assertEquals(
      Ucd.parseUcd("stat.error;phot.mag;em.opt.g"),
      Ucd(
        NonEmptyList.of[NonEmptyString]("stat.error".refined,
                                        "phot.mag".refined,
                                        "em.opt.g".refined
        )
      ).rightNec
    )
  }

  test("parse be case-insensitive, converting to lower case") {
    assertEquals(Ucd.parseUcd("STAT.Error;EM.opt.G"), Ucd("stat.error;em.opt.g"))
  }
}
