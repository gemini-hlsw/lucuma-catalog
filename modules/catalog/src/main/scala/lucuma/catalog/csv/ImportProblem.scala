// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.syntax.all.*
import lucuma.catalog.votable.*
import lucuma.core.enums.CatalogName

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait ImportProblem extends Throwable with Product with Serializable {
  def displayValue: String

  override def toString(): String = displayValue
}

object ImportProblem {
  case object MissingCoordinates       extends ImportProblem {
    val displayValue = "Either Right Ascension or Declination are missing"
  }
  case object UnknownCatalog           extends ImportProblem {
    val displayValue = s"Requested an unknown catalog"
  }
  case class GenericError(msg: String) extends ImportProblem {
    val displayValue = msg
  }
  case class LookupError(msg: String)  extends ImportProblem {
    val displayValue = msg
  }

  case class CatalogException(problems: List[ImportProblem])
      extends RuntimeException(problems.mkString(", ")) {
    def firstMessage: String =
      problems.headOption.map {
        case GenericError(msg) => msg
        case e                 => e.toString
      }.orEmpty
  }
}