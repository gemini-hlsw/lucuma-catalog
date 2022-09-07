// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.kernel.laws.discipline._
import lucuma.catalog.arb.all.given
import lucuma.core.math.Declination
import lucuma.core.math.arb.ArbAngle
import lucuma.core.math.arb.ArbRightAscension.*
import lucuma.core.optics.laws.discipline.FormatTests
import munit._

class AngleParsersSuite extends DisciplineSuite with AngleParsers {
  assert(HourParser.parseAll("0").isRight)
  assert(HourParser.parseAll("09").isRight)
  assert(HourParser.parseAll("1").isRight)
  assert(HourParser.parseAll("01").isRight)
  assert(HourParser.parseAll("10").isRight)
  assert(HourParser.parseAll("19").isRight)
  assert(HourParser.parseAll("22").isRight)
  assert(HourParser.parseAll("25").isLeft)

  assert(MinuteParser.parseAll("0").isRight)
  assert(MinuteParser.parseAll("09").isRight)
  assert(MinuteParser.parseAll("1").isRight)
  assert(MinuteParser.parseAll("01").isRight)
  assert(MinuteParser.parseAll("10").isRight)
  assert(MinuteParser.parseAll("19").isRight)
  assert(MinuteParser.parseAll("22").isRight)
  assert(MinuteParser.parseAll("59").isRight)
  assert(MinuteParser.parseAll("60").isLeft)

  assert(SecondsParser.parseAll("59").isRight)
  assert(SecondsParser.parseAll("59.").isRight)
  assert(SecondsParser.parseAll("59.1").isRight)
  assert(SecondsParser.parseAll("59.00000000000000").isRight)
  assert(SecondsParser.parseAll("59.1234567890").isRight)

  assert(HMSParser.parseAll("00:00:00.000000").isRight)
  assert(HMSParser.parseAll("01:51:51.10").isRight)
  assert(HMSParser.parseAll("01:51:51.100").isRight)
  assert(HMSParser.parseAll("01:51:51.1000").isRight)
  assert(HMSParser.parseAll("01:51:51.1000100").isRight)
  assert(HMSParser.parseAll("14:29:42.9461331854").isRight)
  assert(HMSParser.parseAll("14:29:42").isRight)

  println(HMSParser.parseAll("14h 29m 42s"))
  assert(HMSParser.parseAll("14h 29m 42s").isRight)

  assert(neg.parseAll("-").isRight)
  assert(neg.parseAll("+").isRight)
  assert(neg.parseAll("").isRight)
  assert(DMSParser.parseAll("14:29:42").isRight)
  assert(DMSParser.parseAll("-16:27:46.522175847").isRight)
  println(DMSParser.parseAll("-16:27:46.522175847").map(Declination.fromAngle.getOption))

  // Laws
  checkAll("fromStringHMS", FormatTests(lenientFromStringHMS).formatWith(ArbAngle.stringsHMS))
}
