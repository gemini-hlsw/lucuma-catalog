// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.votable

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import org.openjdk.jmh.annotations.*

import java.util.concurrent.TimeUnit
import lucuma.ags.*
import org.http4s.jdkhttpclient.JdkHttpClient
import lucuma.core.math.Offset
import java.time.Instant

@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 1)
@Measurement(iterations = 10, time = 1)
class AgsBenchmark extends AgsSelectionSample {

  // var items: Vector[GuideStarCandidate] = Vector.empty
  var items: List[GuideStarCandidate] = Nil

  @Setup
  def simpleRun: Unit =
    items = JdkHttpClient
      .simple[IO]
      .flatMap(
        gaiaQuery[IO](_)
          .map(GuideStarCandidate.siderealTarget.get)
          .compile
          .toList
      )
      .flatTap(x => IO.println(s"Loaded ${x.length}"))
      .unsafeRunSync()

  @Benchmark
  def ags: Unit = {
    Ags.agsAnalysis(
      constraints,
      wavelength,
      coords,
      List(coords),
      positions,
      gmosParams,
      items
    )
    ()
  }

  @Benchmark
  def agsPM: Unit = {
    Ags.agsAnalysisPM(
      constraints,
      wavelength,
      _ => coords.some,
      List(_ => coords.some),
      positions,
      gmosParams,
      Instant.now(),
      items
    )
    ()
  }

  @Benchmark
  def magnitudeAnalysis: Unit = {
    val geoms  = gmosParams.posCalculations(NonEmptyList.one(positions.head))
    val limits = Ags.guideSpeedLimits(constraints, wavelength)
    Ags.magnitudeAnalysis(
      constraints,
      gmosParams.probe,
      Offset.Zero,
      items.head,
      geoms.get(0).get.vignettingArea(_),
      positions.head
    )(limits)
    ()
  }
}

// Baseline ags
//
// [info] Result "lucuma.catalog.AgsBenchmark.ags":
// [info]   0.004 ±(99.9%) 0.001 ops/ms [Average]
// [info]   (min, avg, max) = (0.004, 0.004, 0.004), stdev = 0.001
// [info]   CI (99.9%): [0.004, 0.004] (assumes normal distribution)
//
// Coulomb calcs
// [info] Result "lucuma.catalog.AgsBenchmark.ags":
// [info]   0.007 ±(99.9%) 0.001 ops/ms [Average]
// [info]   (min, avg, max) = (0.007, 0.007, 0.007), stdev = 0.001
// [info]   CI (99.9%): [0.007, 0.007] (assumes normal distribution)
//
// Cache geometry calculations
// [info] Result "lucuma.catalog.AgsBenchmark.ags":
// [info]   0.055 ±(99.9%) 0.001 ops/ms [Average]
// [info]   (min, avg, max) = (0.054, 0.055, 0.056), stdev = 0.001
// [info]   CI (99.9%): [0.054, 0.056] (assumes normal distribution)
//
// Baseline mag
//
// [info] Result "lucuma.catalog.AgsBenchmark.magnitudeAnalysis":
// [info]   64.509 ±(99.9%) 0.452 ops/ms [Average]
// [info]   (min, avg, max) = (63.972, 64.509, 64.768), stdev = 0.299
// [info]   CI (99.9%): [64.057, 64.961] (assumes normal distribution)
//
// Coulomb calcs
// [info] Result "lucuma.catalog.AgsBenchmark.magnitudeAnalysis":
// [info]   986.528 ±(99.9%) 3.535 ops/ms [Average]
// [info]   (min, avg, max) = (981.314, 986.528, 988.987), stdev = 2.338
// [info]   CI (99.9%): [982.993, 990.063] (assumes normal distribution)
