// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog.votable

trait VoTableSamples {

  val fieldsNode =
    <TABLE>
        <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
        <FIELD ID="rmag_err" datatype="double" name="rmag_err" ucd="stat.error;phot.mag;em.opt.r"/>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
      </TABLE>

  val tableRow =
    <TR>
        <TD>0.0960165</TD>
        <TD>0.0503736</TD>
        <TD>268435728</TD>
        <TD>-2140405448</TD>
      </TR>

  val dataNode =
    <DATA>
        <TABLEDATA>
          <TR>
            <TD>0.0960165</TD>
            <TD>0.0503736</TD>
            <TD>268435728</TD>
            <TD>-2140405448</TD>
          </TR>
          <TR>
            <TD>0.51784</TD>
            <TD>0.252201</TD>
            <TD>536871168</TD>
            <TD>-2140404569</TD>
          </TR>
        </TABLEDATA>
      </DATA>

  val skyObjects =
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
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>268435728</TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.209323681906</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD>19.8812</TD>
              <TD>3</TD>
              <TD>-2140405448</TD>
            </TR>
            <TR>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.210251239819</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
              <TD>-2140404569</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

  val skyObjectsWithRedshift =
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
        <FIELD ID="Z_VALUE" datatype="double" name="Z_VALUE" ucd="src.redshift"/>
        <FIELD ID="RV_VALUE" datatype="double" name="RV_VALUE" ucd="spect.dopplerVeloc.opt"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>268435728</TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.209323681906</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD>19.8812</TD>
              <TD>3</TD>
              <TD>-2140405448</TD>
              <TD>0.000068</TD>
              <TD></TD>
            </TR>
            <TR>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.210251239819</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
              <TD>-2140404569</TD>
              <TD></TD>
              <TD>20.30</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

  val skyObjectsWithRadialVelocityError =
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
        <FIELD ID="RV_VALUE" datatype="double" name="RV_VALUE" ucd="spect.dopplerVeloc.opt"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.210251239819</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
              <TD>-2140404569</TD>
              <TD>299792.459</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

  val skyObjectsWithErrors =
    <TABLE>
        <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
        <FIELD ID="rmag_err" datatype="double" name="rmag_err" ucd="stat.error;phot.mag;em.opt.r"/>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="zmag_err" datatype="double" name="zmag_err" ucd="stat.error;phot.mag;em.opt.z"/>
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="umag_err" datatype="double" name="umag_err" ucd="stat.error;phot.mag;em.opt.u"/>
        <FIELD ID="imag_err" datatype="double" name="imag_err" ucd="stat.error;phot.mag;em.opt.i"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="jmag" datatype="double" name="jmag" ucd="phot.mag;em.IR.J"/>
        <FIELD ID="e_jmag" datatype="double" name="e_jmag" ucd="stat.error;phot.mag;em.IR.J"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>0.0960165</TD>
              <TD>0.0503736</TD>
              <TD>268435728</TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.138202</TD>
              <TD>0.209323681906</TD>
              <TD>0.518214</TD>
              <TD>0.0456069</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD>19.8812</TD>
              <TD>3</TD>
              <TD>13.74</TD>
              <TD>0.029999999999999999</TD>
              <TD>-2140405448</TD>
            </TR>
            <TR>
              <TD>0.51784</TD>
              <TD>0.252201</TD>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.35873</TD>
              <TD>0.210251239819</TD>
              <TD>1.20311</TD>
              <TD>0.161275</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
              <TD>12.023</TD>
              <TD>0.02</TD>
              <TD>-2140404569</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

  val skyObjectsWithProperMotion =
    <TABLE>
        <FIELD ID="pmde" datatype="double" name="pmde" ucd="pos.pm;pos.eq.dec"/>
        <FIELD ID="pmra" datatype="double" name="pmra" ucd="pos.pm;pos.eq.ra"/>
        <FIELD ID="dej2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="epde" datatype="double" name="epde" ucd="time.epoch"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.R"/>
        <FIELD ID="e_vmag" datatype="double" name="e_vmag" ucd="stat.error;phot.mag;em.opt.V"/>
        <FIELD ID="e_pmra" datatype="double" name="e_pmra" ucd="stat.error;pos.pm;pos.eq.ra"/>
        <FIELD ID="ucac4" arraysize="*" datatype="char" name="ucac4" ucd="meta.id;meta.main"/>
        <FIELD ID="epra" datatype="double" name="epra" ucd="time.epoch"/>
        <FIELD ID="e_pmde" datatype="double" name="e_pmde" ucd="stat.error;pos.pm;pos.eq.dec"/>
        <DATA>
         <TABLEDATA>
          <TR>
           <TD>-4.9000000000000004</TD>
           <TD>-10.199999999999999</TD>
           <TD>19.9887894444444</TD>
           <TD>2000.3499999999999</TD>
           <TD>9.8971419444444404</TD>
           <TD>14.76</TD>
           <TD>0.02</TD>
           <TD>3.8999999999999999</TD>
           <TD>550-001323</TD>
           <TD>1999.9100000000001</TD>
            <TD>2.4345</TD>
          </TR>
          <TR>
           <TD>-13.9</TD>
           <TD>-7</TD>
           <TD>19.997709722222201</TD>
           <TD>2000.0699999999999</TD>
           <TD>9.9195805555555605</TD>
           <TD>12.983000000000001</TD>
           <TD>0.029999999999999999</TD>
           <TD>1.8</TD>
           <TD>550-001324</TD>
           <TD>1999.4300000000001</TD>
           <TD>2.3999999999999999</TD>
          </TR>
         </TABLEDATA>
        </DATA>
       </TABLE>

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

  val voTable =
    <VOTABLE>
        <RESOURCE type="results">
          {skyObjects}
        </RESOURCE>
      </VOTABLE>

  val voTableWithErrors =
    <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithErrors}
        </RESOURCE>
      </VOTABLE>

  val voTableWithProperMotion =
    <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithProperMotion}
        </RESOURCE>
      </VOTABLE>

  val voTableWithRedshift =
    <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithRedshift}
        </RESOURCE>
      </VOTABLE>

  val voTableWithRadialVelocityError =
    <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithRadialVelocityError}
        </RESOURCE>
      </VOTABLE>

  val voTableGaia =
    <VOTABLE>
        <RESOURCE type="results">
          {gaia}
        </RESOURCE>
      </VOTABLE>

}
