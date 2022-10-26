// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.parse.Parser.char
import cats.parse.Parser.charIn
import cats.parse.Parser.string
import cats.parse.Rfc5234.*
import cats.parse.*
import lucuma.core.math.Angle
import lucuma.core.math.Declination
import lucuma.core.math.HourAngle
import lucuma.core.math.RightAscension
import lucuma.core.optics.Format
import org.typelevel.ci.*

// TODO Move to core
trait AngleParsers:
  val colon        = char(':')
  val colonOrSpace = char(':') | sp

  val HourParser = (char('1') ~ digit).backtrack // 10-19
    .orElse(char('2') ~ charIn('0' to '4')) // 20-24
    .backtrack
    .orElse(char('0').? *> digit)           // 00 - 09
    .backtrack
    .orElse(char('0'))                      // plain 0
    .string
    .map(_.toInt)

  val MinuteParser = (charIn('0' to '5') ~ digit).backtrack // 10-19
    .orElse(char('5') ~ digit)          // 20-24
    .backtrack
    .orElse(char('0').?.with1 *> digit) // 00 - 09
    .backtrack
    .orElse(char('0'))                  // plain 0
    .string
    .map(_.toInt)

  val SecondsParser =
    // allow any decimals but only use 6
    (MinuteParser ~ char('.').? ~ digit.rep(1, 3).?.string ~ digit.rep(1, 3).?.string ~ digit.rep.?)
      .map { case ((((s, _), d1), d2), _) =>
        (s,
         if (d1.isEmpty) BigDecimal(0) else BigDecimal(d1.padTo(3, '0')),
         if (d2.isEmpty) BigDecimal(0) else BigDecimal(d2)
        )
      }

  private val HMSParser1 =
    (HourParser ~ colonOrSpace.void ~ MinuteParser ~ colonOrSpace.void ~ SecondsParser).map {
      case ((((h, _), m), _), (s, ms, µs)) =>
        HourAngle.fromHMS(h, m, s, ms.toInt, µs.toInt)
    }

  private val HMSParser2 =
    (HourParser ~ (char('h') ~ sp.?).void ~
      MinuteParser ~ (char('m') ~ sp.?).void ~
      SecondsParser ~ char('s'))
      .map { case (((((h, _), m), _), (s, ms, µs)), _) =>
        HourAngle.fromHMS(h, m, s, ms.toInt, µs.toInt)
      }

  val HMSParser = HMSParser1.backtrack | HMSParser2

  val neg = char('-').map(_ => true).backtrack | char('+').?.map(_ => false)

  val DegreesParser = (charIn('0' to '9') ~ digit).backtrack // 10-19
    .backtrack
    .orElse(char('0').? *> digit) // 00 - 09
    .backtrack
    .orElse(char('0'))            // plain 0
    .string
    .map(_.toInt)

  private val DMSParser1 =
    (neg ~ DegreesParser ~ colonOrSpace ~ MinuteParser ~ colonOrSpace ~ SecondsParser).map {
      case (((((neg, h), _), m), _), (s, ms, µs)) =>
        val r = Angle.fromDMS(h, m, s, ms.toInt, µs.toInt)
        if (neg) -r else r
    }

  private val DMSParser2 =
    (neg ~ DegreesParser ~ (char('°') ~ sp.?).void ~
      MinuteParser ~ (char('′') ~ sp.?).void ~
      SecondsParser ~ char('″').void).map { case ((((((neg, h), _), m), _), (s, ms, µs)), _) =>
      val r = Angle.fromDMS(h, m, s, ms.toInt, µs.toInt)
      if (neg) -r else r
    }

  val DMSParser = DMSParser1.backtrack | DMSParser2

  val lenientFromStringHMS: Format[String, RightAscension] =
    Format[String, RightAscension](
      HMSParser.parseAll(_).toOption.map(RightAscension.fromHourAngle.get),
      RightAscension.fromStringHMS.reverseGet
    )

  val lenientFromStringDMS: Format[String, Declination] =
    Format[String, Declination](
      DMSParser.parseAll(_).toOption.flatMap(Declination.fromAngle.getOption),
      Declination.fromStringSignedDMS.reverseGet
    )
