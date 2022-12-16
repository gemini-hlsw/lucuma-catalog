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
import lucuma.core.math.Epoch
import lucuma.core.math.RightAscension
import lucuma.core.model.SourceProfile
import lucuma.core.model.Target
import munit.CatsEffectSuite
import org.http4s.jdkhttpclient.JdkHttpClient
import org.typelevel.ci.*
import lucuma.core.enums.Band

class TargetImportFileSuite extends CatsEffectSuite:

  test("parse sample pit file with spaces") {
    val xmlFile = "/targets_test_case.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 1)
          assertEquals(
            l.head.toOption
              .flatMap(t => Target.integratedBrightnessIn(Band.V).headOption(t))
              .map(_.value),
            BigDecimal(10).some
          )
        }
    }
  }

  test("parse sample pit file") {
    val xmlFile = "/PIT_sidereal.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 6)
          assertEquals(l.count {
                         case Right(Target.Sidereal(_, _, SourceProfile.Uniform(_), _)) => true
                         case _                                                         => false
                       },
                       1
          )
        }
    }
  }

  test("parse sample pit file with mixed units") {
    val xmlFile = "/PIT_sidereal_mixed_units.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 1)
          assertEquals(l.count {
                         case Left(_) => true
                         case _       => false
                       },
                       1
          )
        }
    }
  }

  test("parse stars sample file") {
    val xmlFile = "/stars.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 20)
          assertEquals(l.count(_.isRight), 20)
        }
    }
  }

  test("parse stars with pm file") {
    val xmlFile = "/stars_pm.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 19)
          assertEquals(l.count(_.isRight), 19)
          assertEquals(l.count(_.exists(_.tracking.properMotion.isDefined)), 5)
        }
    }
  }

  test("parse stars with pm and epoch file") {
    val xmlFile = "/stars_pm_epoch.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        // .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 19)
          assertEquals(l.count(_.isRight), 18)
          assertEquals(l.count(_.exists(_.tracking.properMotion.isDefined)), 5)
          assertEquals(l.count(_.exists(_.tracking.epoch =!= Epoch.J2000)), 6)
          assertEquals(l.count(_.isLeft), 1)
          assertEquals(
            l.find(_.isLeft).map(_.leftMap(_.toList)),
            List(
              ImportProblem.CsvParsingError("Invalid epoch value 'J123' in line 17", 17L.some)
            ).asLeft.some
          )
        }
    }
  }

  test("parse stars sample file with errors") {
    val xmlFile = "/stars_with_errors.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        .map { l =>
          assertEquals(l.length, 20)
          assertEquals(l.count(_.isRight), 19)
          assertEquals(l.count(_.isLeft), 1)
          assertEquals(
            l.find(_.isLeft).map(_.leftMap(_.toList)),
            List(
              ImportProblem.CsvParsingError("Invalid RA value '    a' in line 21", 21L.some)
            ).asLeft.some
          )
        }
    }
  }

  test("parse stars random file") {
    val xmlFile = "/random.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        .map { l =>
          assertEquals(l.length, 1000)
          assertEquals(l.count(_.isRight), 1000)
        }
    }
  }

  test("parse names file with lookup") {
    val xmlFile = "/target_names.csv"
    val file    = getClass().getResource(xmlFile)
    JdkHttpClient
      .simple[IO]
      .flatMap { client =>
        Files[IO]
          .readAll(Path(file.getPath()))
          .through(text.utf8.decode)
          .through(TargetImport.csv2targetsAndLookup(client))
          .compile
          .toList
          // .flatTap(x => IO(pprint.pprintln(x)))
          .map { l =>
            assertEquals(l.length, 7)
            assertEquals(l.count(_.isRight), 4)
            assertEquals(l.count(_.isLeft), 3)
          }
      }
  }
