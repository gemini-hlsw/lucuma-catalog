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
import org.http4s.jdkhttpclient.JdkHttpClient
import org.typelevel.ci.*

class TargetImportFileSuite extends CatsEffectSuite:

  test("parse sample pit file".ignore) {
    val xmlFile = "/PIT_sidereal.csv"
    val file    = getClass().getResource(xmlFile)
    Resource.unit[IO].use { _ =>
      Files[IO]
        .readAll(Path(file.getPath()))
        .through(text.utf8.decode)
        .through(TargetImport.csv2targets)
        .compile
        .toList
        .map { l =>
          assertEquals(l.length, 5)
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
        .map { l =>
          assertEquals(l.length, 20)
          assertEquals(l.count(_.isRight), 20)
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
        .flatTap(x => IO(pprint.pprintln(x)))
        .map { l =>
          assertEquals(l.length, 20)
          assertEquals(l.count(_.isRight), 19)
          assertEquals(l.count(_.isLeft), 1)
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
      .use { client =>
        Files[IO]
          .readAll(Path(file.getPath()))
          .through(text.utf8.decode)
          .through(TargetImport.csv2targetsAndLookup(client))
          .compile
          .toList
          // .flatTap(x => IO(pprint.pprintln(x)))
          .map { l =>
            assertEquals(l.length, 5)
          }
      }
  }