// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.implicits._

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait CatalogProblem extends Throwable with Product with Serializable {
  def displayValue: String
}

case class ValidationError(catalog: CatalogName) extends CatalogProblem {
  val displayValue = s"Invalid response from $catalog"
}
case class InvalidFieldId(id: String) extends CatalogProblem {
  val displayValue = s"Invalid field id: '$id'"
}
case class UnknownXmlTag(tag: String) extends CatalogProblem {
  val displayValue = s"Unknown tag: '$tag'"
}
case class MissingXmlTag(tag: String) extends CatalogProblem {
  val displayValue = s"Missing tag: '$tag'"
}
case class MissingXmlAttribute(attr: String) extends CatalogProblem {
  val displayValue = s"Missing attr: '$attr'"
}
case class InvalidUcd(ucd: String) extends CatalogProblem {
  val displayValue = s"Invalid ucd: '$ucd'"
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
