// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog

import cats.implicits._

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait CatalogProblem extends Throwable {
  def displayValue: String
}

case class ValidationError(catalog: CatalogName) extends CatalogProblem {
  val displayValue = s"Invalid response from ${catalog.displayName}"
}
case class GenericError(msg: String) extends CatalogProblem {
  val displayValue = msg
}
case class MissingValue(field: FieldId) extends CatalogProblem {
  val displayValue = s"Missing required field ${field.id}"
}
case class FieldValueProblem(ucd: Ucd, value: String) extends CatalogProblem {
  val displayValue = s"Error parsing field $ucd with value $value"
}
case class UnmatchedField(ucd: Ucd) extends CatalogProblem {
  val displayValue = s"Unmatched field $ucd"
}
case object UnknownCatalog extends CatalogProblem {
  val displayValue = s"Requested an unknown catalog"
}

case class CatalogException(problems: List[CatalogProblem])
    extends RuntimeException(problems.mkString(", ")) {
  def firstMessage: String =
    problems.headOption.map {
      case e: GenericError => e.msg
      case e               => e.toString
    }.orEmpty
}
