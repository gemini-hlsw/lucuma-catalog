// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import lucuma.ags._
import lucuma.core.model.ConstraintSet
import lucuma.core.enums._
import lucuma.core.math.Wavelength
import lucuma.core.model.ElevationRange
import org.http4s.jdkhttpclient.JdkHttpClient
import lucuma.core.math.Angle
import lucuma.core.math.Offset
import lucuma.core.geom.jts.interpreter._
import lucuma.core.geom.Area

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
      .use(
        gaiaQuery[IO](_, widestConstraints)
          .map(GuideStarCandidate.siderealTarget.get)
          .compile
          .toList
      )
      .flatTap(x => IO.println(s"Loaded ${x.length}"))
      .unsafeRunSync()

  val constraints = ConstraintSet(ImageQuality.PointTwo,
                                  CloudExtinction.PointFive,
                                  SkyBackground.Dark,
                                  WaterVapor.Wet,
                                  ElevationRange.AirMass.Default
  )

  val wavelength = Wavelength.fromNanometers(700).get

  val params = AgsParams.GmosAgsParams(
    GmosNorthFpu.LongSlit_5_00.asLeft.some,
    PortDisposition.Bottom
  )

  val pos = AgsPosition(Angle.Angle0, Offset.Zero)

  @Benchmark
  def ags: Unit = {
    Ags.agsAnalysis(
      constraints,
      wavelength,
      coords,
      AgsPosition(Angle.Angle0, Offset.Zero),
      AgsParams.GmosAgsParams(
        GmosNorthFpu.LongSlit_5_00.asLeft.some,
        PortDisposition.Bottom
      ),
      items
    )
    ()
  }

  // @Benchmark
  // def magnitudeAnalysis: Unit = {
  //   Ags.magnitudeAnalysis(
  //     constraints,
  //     params.probe,
  //     Offset.Zero,
  //     items.head,
  //     wavelength,
  //     // _ => Area.MinArea
  //     params.vignettingArea(pos)(_).eval.area
  //   )
  //   ()
  // }
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
