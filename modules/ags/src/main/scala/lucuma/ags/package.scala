// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma

import cats.syntax.all._
import algebra.instances.all.given
import _root_.refined.algebra._
import coulomb.units.accepted.ArcSecond
import coulomb.*
import coulomb.ops.algebra.spire.all.given
import coulomb.policy.spire.standard.given
import coulomb.syntax.*
import coulomb.units.si.*
import coulomb.units.si.given
import lucuma.catalog.BandsList
import lucuma.catalog.BrightnessConstraints
import lucuma.catalog.FaintnessConstraint
import lucuma.catalog.SaturationConstraint
import lucuma.core.enums.CloudExtinction
import lucuma.core.enums.GuideSpeed
import lucuma.core.enums.ImageQuality
import lucuma.core.enums.SkyBackground
import lucuma.core.math.Wavelength
import lucuma.core.model.ConstraintSet
import lucuma.core.util.Enumerated
// import spire.math.Rational
// import spire.std.int._
// import lucuma.core.math.units.rationalPosIntConverter

import scala.math

package object ags {
  val baseFwhm = Wavelength.fromNanometers(500).get

  // FWHM as seen on the optical wavefront sensor (WFS)
  // Operate on Double, we don't need exact precision
  def wfsFwhm(sciFwhm: ImageQuality, wavelength: Wavelength): Double = {
    val coeff =
      baseFwhm.toPicometers.value.value.toDouble / wavelength.toPicometers.value.value.toDouble
    (sciFwhm.toDeciArcSeconds.value.value / 10.0) * math.pow(coeff, -0.2)

  }

  // Calculate the widest set of constraints, useful to cache catalog results
  // We get the union off all possible constraints at the slow guide speed
  // darkest sky background and 300nm wavelength
  val widestConstraints: BrightnessConstraints = {
    val widest = Wavelength.fromNanometers(300).get

    val constraints = Enumerated[ImageQuality].all.flatMap { iq =>
      Enumerated[CloudExtinction].all.map { ce =>
        faintLimit(GuideSpeed.Slow, widest, SkyBackground.Darkest, iq, ce)
      }
    }
    // The list is never empty
    val lowest      = constraints.maximumOption.get
    BrightnessConstraints(BandsList.GaiaBandsList, lowest, none)
  }

  /**
   * Calculates the daintness limits for a given Guide Speed/Wavelength and conditions These are
   * based on the gaia G band and described here:
   */
  def faintLimit(
    guideSpeed: GuideSpeed,
    wavelength: Wavelength,
    sb:         SkyBackground,
    iq:         ImageQuality,
    ce:         CloudExtinction
  ): FaintnessConstraint = {
    val limit = sb match {
      case SkyBackground.Darkest =>
        guideSpeed match {
          case GuideSpeed.Fast   =>
            16.4 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Medium =>
            16.9 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Slow   =>
            17.4 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
        }
      case SkyBackground.Dark    =>
        guideSpeed match {
          case GuideSpeed.Fast   =>
            16.3 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Medium =>
            16.8 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Slow   =>
            17.3 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
        }
      case SkyBackground.Gray    =>
        guideSpeed match {
          case GuideSpeed.Fast   =>
            16.2 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Medium =>
            16.7 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Slow   =>
            17.2 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
        }
      case SkyBackground.Bright  =>
        guideSpeed match {
          case GuideSpeed.Fast   =>
            16.1 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Medium =>
            16.6 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
          case GuideSpeed.Slow   =>
            17.1 - 0.8 * wfsFwhm(iq, wavelength) - ce.toBrightness
        }
    }
    FaintnessConstraint(limit)
  }

  def gaiaBrightnessConstraints(
    guideSpeed: GuideSpeed,
    wavelength: Wavelength,
    sb:         SkyBackground,
    iq:         ImageQuality,
    ce:         CloudExtinction
  ): BrightnessConstraints = {
    val faintness  = faintLimit(guideSpeed, wavelength, sb, iq, ce)
    val saturation = SaturationConstraint(faintness.brightness - 6)
    BrightnessConstraints(BandsList.GaiaBandsList, faintness, saturation.some)
  }

  def gaiaBrightnessConstraints(
    constraints: ConstraintSet,
    guideSpeed:  GuideSpeed,
    wavelength:  Wavelength
  ): BrightnessConstraints =
    gaiaBrightnessConstraints(guideSpeed,
                              wavelength,
                              constraints.skyBackground,
                              constraints.imageQuality,
                              constraints.cloudExtinction
    )

}
