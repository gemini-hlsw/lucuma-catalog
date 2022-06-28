// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Eq
import cats.syntax.all._
import coulomb._
import eu.timepit.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog.BandsList
import lucuma.core.enums.Band
import lucuma.core.enums.StellarLibrarySpectrum
import lucuma.core.math.BrightnessUnits._
import lucuma.core.math.dimensional._
import lucuma.core.math.units._
import lucuma.core.model.SiderealTracking
import lucuma.core.model.SourceProfile
import lucuma.core.model.SpectralDefinition
import lucuma.core.model.Target
import lucuma.core.model.UnnormalizedSED
import lucuma.core.optics.SplitEpi

import scala.collection.immutable.SortedMap

/**
 * Poors' man Target.Sidereal with a single G brightness and no extra metadata
 */
final case class GuideStarCandidate(
  id:          Long,
  tracking:    SiderealTracking,
  gBrightness: Option[BigDecimal]
) {
  def name: NonEmptyString =
    refineV[NonEmpty](s"Gaia DR2 $id").getOrElse(sys.error("Cannot happen"))
}

object GuideStarCandidate {
  implicit val eqGuideStar: Eq[GuideStarCandidate] = Eq.by(x => (x.name, x.tracking, x.gBrightness))

  val GaiaNameRegex = """Gaia DR2 (-?\d*)""".r

  // There is some loss of info converting one to the other but further
  // conversions are always the same, thus SplitEpi
  val siderealTarget: SplitEpi[Target.Sidereal, GuideStarCandidate] =
    SplitEpi(
      st => {
        val gBrightness = BandsList.GaiaBandsList.bands
          .flatMap { band =>
            SourceProfile.integratedBrightnessIn(band).headOption(st.sourceProfile)
          }
          .headOption
          .map(_.value)

        GuideStarCandidate(
          st.name.value match {
            case GaiaNameRegex(d) => d.toLong
            case _                => -1
          },
          st.tracking,
          gBrightness
        )
      },
      g =>
        Target.Sidereal(
          g.name,
          g.tracking,
          SourceProfile.Point(
            SpectralDefinition.BandNormalized(
              UnnormalizedSED.StellarLibrary(StellarLibrarySpectrum.O5V),
              SortedMap.from(
                g.gBrightness
                  .foldMap(g => List(Band.Gaia -> g.withUnit[VegaMagnitude].toMeasureTagged))
                  .toSeq
              )
            )
          ),
          none
        )
    )
}
