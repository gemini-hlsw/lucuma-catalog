// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import fs2.io.file.Files
import fs2.io.file.Path
import lucuma.catalog.*
import lucuma.catalog.csv.TargetImport.given
import lucuma.catalog.csv.*
import lucuma.core.math.Declination
import lucuma.core.math.RightAscension
import munit.CatsEffectSuite
import org.typelevel.ci.*

class TargetImportFileSuite extends CatsEffectSuite:

  // test("parse sample pit file") {
  //   val xmlFile = "/PIT_sidereal.csv"
  //   val file    = getClass().getResource(xmlFile)
  //   Resource.unit[IO].use { _ =>
  //     Files[IO]
  //       .readAll(Path(file.getPath()))
  //       .through(text.utf8.decode)
  //       .through(decodeUsingHeaders[TargetCsvRow]())
  //       .compile
  //       .toList
  //       .flatTap(x => IO(pprint.pprintln(x)))
  //       .map { l =>
  //         assertEquals(l.length, 5)
  //       }
  //   }
  // }
  //
  // test("parse stars sample file".ignore) {
  //   val xmlFile = "/stars.csv"
  //   val file    = getClass().getResource(xmlFile)
  //   Resource.unit[IO].use { _ =>
  //     Files[IO]
  //       .readAll(Path(file.getPath()))
  //       .through(text.utf8.decode)
  //       .through(text.lines)
  //       .map(s => s.split(",").map(_.trim()).mkString("", ",", "\n"))
  //       .through(lowlevel.rows[IO, String]())
  //       .through(lowlevel.headers[IO, CIString])
  //       .through(lowlevel.decodeRow[IO, CIString, TargetCsvRow])
  //       .compile
  //       .toList
  //       .flatTap(x => IO(pprint.pprintln(x)))
  //       .map { l =>
  //         assertEquals(l.length, 20)
  //         assertEquals(l.count(_.bare), 0)
  //       }
  //   }
  // }
  //
  // test("parse stars sample file") {
  //   val xmlFile = "/random.csv"
  //   val file    = getClass().getResource(xmlFile)
  //   Resource.unit[IO].use { _ =>
  //     Files[IO]
  //       .readAll(Path(file.getPath()))
  //       .through(text.utf8.decode)
  //       // .through(text.lines)
  //       .through(decodeUsingHeaders[TargetCsvRow]())
  //       .compile
  //       .toList
  //       .flatTap(x => IO(pprint.pprintln(x)))
  //       .map { l =>
  //         assertEquals(l.length, 1000)
  //         assertEquals(l.count(_.bare), 0)
  //       }
  //   }
  // }

  test("parse names file with lookup") {
    val xmlFile = "/target_names.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(decodeUsingHeaders[TargetCsvRow]())
        .compile
        .toList
        .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 5)
        }
    }
  }
