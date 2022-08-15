// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.syntax.eq._
import lucuma.core.util.Enumerated

/**
 * Enumerated type for guide probe
 */
enum GuideProbe(private val tag: String):
  case AOWFS extends GuideProbe("AOWFS")

  case OIWFS extends GuideProbe("OIWFS")

  case PWFS extends GuideProbe("PWFS")

  /** @group Typeclass Instances */
  given Enumerated[GuideProbe] =
    Enumerated.from[GuideProbe](AOWFS, OIWFS, PWFS).withTag(_.tag)
