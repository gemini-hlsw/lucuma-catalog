// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import munit.FunSuite
import lucuma.core.enum.MagnitudeBand
import cats.data.Validated

class SimbadAdapterSuite extends FunSuite {

  test("be able to map magnitude errors in Simbad") {
    // Magnitude errors in simbad don't include the band in the UCD, we must get it from the ID :(
    val magErrorUcd = Ucd.unsafeFromString("stat.error;phot.mag")
    // FLUX_r maps to r'
    assertEquals(
      CatalogAdapter.Simbad
        .parseMagnitude(FieldId.unsafeFrom("FLUX_ERROR_r", magErrorUcd), "20.3051"),
      Validated.validNec(
        (FieldId.unsafeFrom("FLUX_ERROR_r", magErrorUcd), MagnitudeBand.SloanR, 20.3051)
      )
    )

    // FLUX_R maps to R
    assertEquals(
      CatalogAdapter.Simbad.parseMagnitude(FieldId.unsafeFrom("FLUX_ERROR_R", magErrorUcd),
                                           "20.3051"
      ),
      Validated.validNec(
        (FieldId.unsafeFrom("FLUX_ERROR_R", magErrorUcd), MagnitudeBand.R, 20.3051)
      )
    )
  }
}
