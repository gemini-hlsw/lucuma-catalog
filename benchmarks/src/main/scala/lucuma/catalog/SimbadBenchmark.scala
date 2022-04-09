// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import fs2._

import lucuma.core.enum.CatalogName
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import cats.effect.unsafe.implicits.global

@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 40, time = 1)
class SimbadBenchmark {

  val targets =
    <TABLE>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
        <FIELD ID="MAIN_ID" name="MAIN_ID" datatype="char" width="22" ucd="meta.id;meta.main" arraysize="*"/>
        <FIELD ID="TYPED_ID" name="TYPED_ID" datatype="char" width="25" ucd="meta.id" arraysize="*"/>
        <FIELD ID="RA_d" name="RA_d" datatype="double" precision="8" width="11" ucd="pos.eq.ra;meta.main" unit="deg"/>
        <FIELD ID="DEC_d" name="DEC_d" datatype="double" precision="8" width="12" ucd="pos.eq.dec;meta.main" unit="deg"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>268435728</TD>
              <TD></TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.209323681906</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD></TD>
              <TD>3</TD>
              <TD>-2140405448</TD>
              <TD>Target</TD>
              <TD>20.3051</TD>
              <TD>0.209323681906</TD>
            </TR>
            <TR>
              <TD>536871168</TD>
              <TD></TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.210251239819</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD></TD>
              <TD>3</TD>
              <TD>-2140404569</TD>
              <TD>Target</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

  @Benchmark
  def simpleRun: Unit = {
    Stream.emit(targets.toString)
      .through(VoTableParser.targets[IO](CatalogName.Simbad))
      // .evalMap(IO.println)
      .compile
      .drain
      .unsafeRunSync()

  }
}
