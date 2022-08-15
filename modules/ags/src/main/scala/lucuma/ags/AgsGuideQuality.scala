// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Order
import lucuma.core.util.Enumerated

enum AgsGuideQuality(private val tag: String, val message: String):

  case DeliversRequestedIq
      extends AgsGuideQuality("delivers_requested_id", "Delivers requested IQ.")

  case PossibleIqDegradation
      extends AgsGuideQuality("possible_iq_degradation",
                              "Slower guiding required; may not deliver requested IQ."
      )

  case IqDegradation
      extends AgsGuideQuality("iq_degradation",
                              "Slower guiding required; will not deliver requested IQ."
      )

  case PossiblyUnusable extends AgsGuideQuality("possible_unusable", "May not be able to guide.")

  case Unusable extends AgsGuideQuality("unusable", "Unable to guide.")

end AgsGuideQuality

object AgsGuideQuality:

  private val All: List[AgsGuideQuality] =
    List(DeliversRequestedIq, PossibleIqDegradation, IqDegradation, PossiblyUnusable, Unusable)

  private val orderByIndex = All.zipWithIndex.toMap

  /** @group Typeclass Instances */
  given Order[AgsGuideQuality] =
    Order.by(orderByIndex)

  given Enumerated[AgsGuideQuality] =
    Enumerated
      .from[AgsGuideQuality](DeliversRequestedIq,
                             PossibleIqDegradation,
                             IqDegradation,
                             PossiblyUnusable,
                             Unusable
      )
      .withTag(_.tag)
end AgsGuideQuality
