// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.ags

import cats.Order
import lucuma.core.util.Enumerated

sealed trait AgsGuideQuality extends Product with Serializable {
  def tag: String
  def message: String
}

object AgsGuideQuality {
  case object DeliversRequestedIq   extends AgsGuideQuality {
    override val tag     = "delivers_requested_id"
    override val message = "Delivers requested IQ."
  }
  case object PossibleIqDegradation extends AgsGuideQuality {
    override val tag     = "possible_iq_degradation"
    override val message = "Slower guiding required; may not deliver requested IQ."
  }
  case object IqDegradation         extends AgsGuideQuality {
    override val tag     = "iq_degradation"
    override val message = "Slower guiding required; will not deliver requested IQ."
  }
  case object PossiblyUnusable      extends AgsGuideQuality {
    override val tag     = "possible_unusable"
    override val message = "May not be able to guide."
  }
  case object Unusable              extends AgsGuideQuality {
    override val tag     = "unusable"
    override val message = "Unable to guide."
  }

  val All: List[AgsGuideQuality] =
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
}
