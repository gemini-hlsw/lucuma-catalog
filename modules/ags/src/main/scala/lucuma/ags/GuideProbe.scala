// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import lucuma.core.util.Enumerated

/**
 * Enumerated type for guide probe
 */
enum GuideProbe(private val tag: String) derives Enumerated:
  case AOWFS extends GuideProbe("AOWFS")
  case OIWFS extends GuideProbe("OIWFS")
  case PWFS  extends GuideProbe("PWFS")
