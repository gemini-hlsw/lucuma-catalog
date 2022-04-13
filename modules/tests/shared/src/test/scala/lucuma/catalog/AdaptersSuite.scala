// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import cats.syntax.all._
import coulomb._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import fs2._
import fs2.data.xml._
import lucuma.catalog._
import lucuma.core.enum.Band
import lucuma.core.enum.CatalogName
import lucuma.core.math.BrightnessUnits._
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.Parallax
import lucuma.core.math.ProperMotion
import lucuma.core.math.RadialVelocity
import lucuma.core.math.RightAscension
import lucuma.core.math.dimensional._
import lucuma.core.math.units._
import lucuma.core.model.CatalogInfo
import lucuma.core.model.Target
import munit.CatsEffectSuite

import scala.xml.Utility

class AdaptersSuite extends CatsEffectSuite with VoTableParser with VoTableSamples {

  test("be able to parse a field definition") {
    Stream
      .emits(Utility.trim(gaia).toString)
      .through(events[IO, Char])
      .through(referenceResolver[IO]())
      .through(normalize[IO])
      .through(VoTableParser.xml2targets[IO](CatalogAdapter.Gaia))
      .compile
      .lastOrError
      .map {
        case Right(CatalogTargetResult(t, _)) =>
          assertEquals(t.name, refineMV[NonEmpty]("Gaia DR2 5500810326779190016"))
          assertEquals(t.tracking.epoch.some, Epoch.Julian.fromEpochYears(2015.5))
          assertEquals(
            t.catalogInfo,
            CatalogInfo(CatalogName.Gaia, "Gaia DR2 5500810326779190016")
          )
          // base coordinates
          assertEquals(
            Target.baseRA.getOption(t),
            RightAscension.fromDoubleDegrees(95.98749097569124).some
          )
          assertEquals(
            Target.baseDec.getOption(t),
            Declination.fromDoubleDegrees(-52.741666247338124)
          )
          // proper motions
          assertEquals(
            Target.properMotionRA.getOption(t),
            ProperMotion.RA(6456.withUnit[MicroArcSecondPerYear]).some
          )
          assertEquals(
            Target.properMotionDec.getOption(t),
            ProperMotion.Dec(22438.withUnit[MicroArcSecondPerYear]).some
          )
          assertEquals(
            Target.integratedBrightnessIn(Band.Gaia).headOption(t),
            BigDecimal(14.292543).withUnit[VegaMagnitude].toMeasureTagged.some
          )
          // parallax
          assertEquals(
            Target.parallax.getOption(t).flatten,
            Parallax.milliarcseconds.reverseGet(3.6810721649521616).some
          )
          // radial velocity
          assertEquals(
            Target.radialVelocity.getOption(t).flatten,
            RadialVelocity(20.30.withUnit[KilometersPerSecond])
          )
        case Left(_)                          => fail("Gaia response could not be parsed")
      }
  }
}
