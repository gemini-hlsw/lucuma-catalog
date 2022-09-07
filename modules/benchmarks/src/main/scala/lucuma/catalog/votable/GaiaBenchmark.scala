// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.votable

import cats.effect._
import cats.effect.unsafe.implicits.global
import fs2._
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 1)
@Measurement(iterations = 40, time = 1)
class GaiaBenchmark extends VoTableSamples {

  @Benchmark
  def simpleRun: Unit =
    Stream
      .emit(voTableGaia.toString)
      .through(CatalogSearch.siderealTargets[IO](CatalogAdapter.Gaia))
      // .evalMap(IO.println)
      .compile
      .drain
      .unsafeRunSync()

  @Benchmark
  def guideStars: Unit =
    Stream
      .emit(voTableGaia.toString)
      .through(CatalogSearch.guideStars[IO](CatalogAdapter.Gaia))
      // .evalMap(IO.println)
      .compile
      .drain
      .unsafeRunSync()

}
