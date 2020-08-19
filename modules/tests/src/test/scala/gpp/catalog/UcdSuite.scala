// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog.votable

import munit.DisciplineSuite
import cats.kernel.laws.discipline.EqTests
import gpp.catalog._
import gpp.catalog.arb.ArbUcd._

class UcdSuite extends munit.FunSuite with DisciplineSuite {
  checkAll("Ucd", EqTests[Ucd].eqv)

  test("detect if is a superset") {
    assert(Ucd("stat.error;phot.mag;em.opt.i").includes(UcdWord("phot.mag")))
    assert(Ucd("stat.error;phot.mag;em.opt.i").matches("phot.mag".r))
    assert(Ucd("stat.error;phot.mag;em.opt.i").matches("em.opt.(\\w)".r))
  }
  test("parse empty ucds") {
    assertEquals(Ucd.parseUcd(""), Ucd(List()))
  }
  test("parse single token ucds") {
    assertEquals(Ucd.parseUcd("meta.code"), Ucd(List(UcdWord("meta.code"))))
  }
  test("parse multi token ucds and preserve order") {
    assertEquals(Ucd.parseUcd("stat.error;phot.mag;em.opt.g"),
                 Ucd(List(UcdWord("stat.error"), UcdWord("phot.mag"), UcdWord("em.opt.g")))
    )
  }
  test("parse be case-insensitive, converting to lower case") {
    assertEquals(Ucd.parseUcd("STAT.Error;EM.opt.G"), Ucd("stat.error;em.opt.g"))
  }
}
