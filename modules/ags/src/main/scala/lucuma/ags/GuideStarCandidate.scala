// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Eq
import cats.derived.*
import cats.syntax.all.*
import coulomb.*
import coulomb.ops.algebra.spire.all.given
import coulomb.policy.spire.standard.given
import coulomb.syntax.*
import coulomb.units.si.*
import coulomb.units.si.given
import eu.timepit.refined.*
import eu.timepit.refined.cats.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog.BandsList
import lucuma.core.enums.Band
import lucuma.core.enums.StellarLibrarySpectrum
import lucuma.core.math.BrightnessUnits.*
import lucuma.core.math.BrightnessValue
import lucuma.core.math.Epoch
import lucuma.core.math.dimensional.*
import lucuma.core.math.units.*
import lucuma.core.model.SiderealTracking
import lucuma.core.model.SourceProfile
import lucuma.core.model.SpectralDefinition
import lucuma.core.model.Target
import lucuma.core.model.UnnormalizedSED
import lucuma.core.optics.SplitEpi
import monocle.Focus
import monocle.Lens

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import scala.collection.immutable.SortedMap

/**
 * Poors' man Target.Sidereal with a single G brightness and no extra metadata
 */
case class GuideStarCandidate(
  id:          Long,
  tracking:    SiderealTracking,
  gBrightness: Option[BrightnessValue]
) derives Eq {
  def name: NonEmptyString =
    refineV[NonEmpty](s"Gaia DR3 $id").getOrElse(sys.error("Cannot happen"))

  // Reset the candidate to a given instant
  // This can be used to calculate and cache the location base on proper motion
  // The tracking variables are reset to match the epoch to the instant
  def at(i: Instant): GuideStarCandidate = {
    val ldt   = LocalDateTime.ofInstant(i, GuideStarCandidate.UTC)
    val epoch = Epoch.Julian.fromLocalDateTime(ldt).getOrElse(tracking.epoch)
    copy(tracking = tracking.at(i).fold(tracking) { c =>
      val update = SiderealTracking.baseCoordinates.replace(c) >>> SiderealTracking.epoch
        .replace(epoch)
      update(tracking)
    })
  }
}

object GuideStarCandidate {

  val UTC = ZoneId.of("UTC")

  val GaiaNameRegex = """Gaia DR3 (-?\d*)""".r

  val id: Lens[GuideStarCandidate, Long] =
    Focus[GuideStarCandidate](_.id)

  val tracking: Lens[GuideStarCandidate, SiderealTracking] =
    Focus[GuideStarCandidate](_.tracking)

  val gBrightness: Lens[GuideStarCandidate, Option[BrightnessValue]] =
    Focus[GuideStarCandidate](_.gBrightness)

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
              None,
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
