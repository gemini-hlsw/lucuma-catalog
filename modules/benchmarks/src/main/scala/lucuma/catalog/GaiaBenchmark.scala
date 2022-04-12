// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.effect._
import fs2._

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import cats.effect.unsafe.implicits.global

@State(Scope.Thread)
@Fork(1)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 4, time = 1)
@Measurement(iterations = 40, time = 1)
class GaiaBenchmark {

  val gaia =
    <TABLE>
        <FIELD arraysize="*" datatype="char" name="designation" ucd="meta.id;meta.main">
          <DESCRIPTION>Unique source designation (unique across all Data Releases)</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ra" ref="GAIADR2" ucd="pos.eq.ra;meta.main" unit="deg" utype="Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1">
          <DESCRIPTION>Right ascension</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ra_error" ucd="stat.error;pos.eq.ra" unit="mas">
          <DESCRIPTION>Standard error of right ascension</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="dec" ref="GAIADR2" ucd="pos.eq.dec;meta.main" unit="deg" utype="Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2">
          <DESCRIPTION>Declination</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="dec_error" ucd="stat.error;pos.eq.dec" unit="mas">
          <DESCRIPTION>Standard error of declination</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="parallax" ucd="pos.parallax" unit="mas">
          <DESCRIPTION>Parallax</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmra" ucd="pos.pm;pos.eq.ra" unit="mas.yr**-1">
          <DESCRIPTION>Proper motion in right ascension direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmra_error" ucd="stat.error;pos.pm;pos.eq.ra" unit="mas.yr**-1">
          <DESCRIPTION>Standard error of proper motion in right ascension direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmdec" ucd="pos.pm;pos.eq.dec" unit="mas.yr**-1">
          <DESCRIPTION>Proper motion in declination direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmdec_error" ucd="stat.error;pos.pm;pos.eq.dec" unit="mas.yr**-1">
          <DESCRIPTION>Standard error of proper motion in declination direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ref_epoch" ucd="meta.ref;time.epoch" unit="yr">
          <DESCRIPTION>Reference epoch</DESCRIPTION>
        </FIELD>
        <FIELD datatype="float" name="phot_g_mean_mag" ucd="phot.mag;stat.mean;em.opt" unit="mag">
          <DESCRIPTION>G-band mean magnitude</DESCRIPTION>
        </FIELD>
        <FIELD datatype="float" name="bp_rp" ucd="phot.color" unit="mag">
          <DESCRIPTION>BP - RP colour</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="radial_velocity" ucd="spect.dopplerVeloc.opt" unit="km.s**-1">
          <DESCRIPTION>Radial velocity</DESCRIPTION>
        </FIELD>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>Gaia DR2 5500810292414804352</TD>
              <TD>95.97543693997628</TD>
              <TD>0.8972436225190542</TD>
              <TD>-52.74602088557901</TD>
              <TD>1.1187287208599193</TD>
              <TD>-0.059333971256738484</TD>
              <TD>5.444032860309618</TD>
              <TD>2.0096218591421637</TD>
              <TD>2.412759805075276</TD>
              <TD>2.292112882376078</TD>
              <TD>2015.5</TD>
              <TD>19.782911</TD>
              <TD></TD> <!-- No BP - RP means no magnitude information -->
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810842175280768</TD>
              <TD>96.07794677734371</TD>
              <TD>1.7974083970121115</TD>
              <TD>-52.752866472994484</TD>
              <TD>1.3361631129404261</TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD>2015.5</TD>
              <TD></TD> <!-- No G-band means no magnitude information -->
              <TD></TD>
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810223699979264</TD>
              <TD>95.96329279548434</TD>
              <TD>0.01360005536042634</TD>
              <TD>-52.77304994651542</TD>
              <TD>0.01653042640473304</TD>
              <TD>1.0777658952216769</TD>
              <TD>-0.8181139364821904</TD>
              <TD>0.028741305378710533</TD>
              <TD>12.976157539714205</TD>
              <TD>0.031294621220519486</TD>
              <TD>2015.5</TD>
              <TD>13.91764</TD>
              <TD>2.68324375</TD>
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810326779190016</TD>
              <TD>95.98749097569124</TD>
              <TD>0.0862887211183082</TD>
              <TD>-52.741666247338124</TD>
              <TD>0.09341802945058283</TD>
              <TD>3.6810721649521616</TD>
              <TD>6.456830239423608</TD>
              <TD>0.19897351485381112</TD>
              <TD>22.438383124975978</TD>
              <TD>0.18174463860202664</TD>
              <TD>2015.5</TD>
              <TD>14.292543</TD>
              <TD>1.0745363</TD>
              <TD>20.30</TD>  <!-- Radial velocity -->
            </TR>
          </TABLEDATA>
        </DATA>
      </TABLE>

  val voTableGaia =
    <VOTABLE>
        <RESOURCE type="results">
          {gaia}
        </RESOURCE>
      </VOTABLE>

  @Benchmark
  def simpleRun: Unit =
    Stream
      .emit(voTableGaia.toString)
      .through(CatalogSearch.siderealTargets[IO](CatalogAdapter.Gaia))
      // .evalMap(IO.println)
      .compile
      .drain
      .unsafeRunSync()

}
