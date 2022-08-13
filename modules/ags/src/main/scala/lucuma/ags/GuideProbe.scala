// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.syntax.eq._
import lucuma.core.util.Enumerated

/**
 * Enumerated type for guide probe
 */
sealed abstract class GuideProbe(val tag: String) extends Product with Serializable

object GuideProbe {

  /** @group Constructors */
  case object AOWFS extends GuideProbe("AOWFS")

  /** @group Constructors */
  case object OIWFS extends GuideProbe("OIWFS")

  /** @group Constructors */
  case object PWFS extends GuideProbe("PWFS")

  /** @group Typeclass Instances */
  given Enumerated[GuideProbe] =
    Enumerated.from[GuideProbe](AOWFS, OIWFS, PWFS).withTag(_.tag)

}
