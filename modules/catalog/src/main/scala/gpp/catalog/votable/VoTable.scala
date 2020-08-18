// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog.votable

import cats._
import cats.implicits._
import scala.util.matching.Regex
import gem.Target

case class UcdWord(token: String)
case class Ucd(tokens: List[UcdWord]) {
  def includes(ucd: UcdWord): Boolean = tokens.contains(ucd)
  def matches(r:    Regex): Boolean   = tokens.exists(t => r.findFirstIn(t.token).isDefined)
  override def toString = tokens.map(_.token).mkString(", ")
}

object Ucd          {
  def parseUcd(v: String): Ucd =
    Ucd(v.split(";").filter(_.nonEmpty).map(_.toLowerCase).map(UcdWord).toList)

  def apply(ucd: String): Ucd = parseUcd(ucd)
}

/** Describes a field */
case class FieldId(id: String, ucd: Ucd)
case class FieldDescriptor(id: FieldId, name: String)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap: Map[FieldId, String] = items.map(i => i.field.id -> i.data).toMap
}

/** ParsedTable and ParsedResources contains a list of problems */
case class ParsedTable(rows: List[Either[CatalogProblem, Target]]) {
  def containsError: Boolean = rows.exists(_.isLeft)
}

object ParsedTable  {
  val Zero = ParsedTable(Nil)

  implicit val monoid =
    Monoid.instance[ParsedTable](ParsedTable.Zero, (a, b) => ParsedTable(a.rows |+| b.rows))
}

case class ParsedVoResource(tables: List[ParsedTable]) {
  def containsError: Boolean = tables.exists(_.containsError)
}

/** The result of parsing a Catalog Query is a list of targets */
case class TargetsTable(rows: List[Target])

object TargetsTable {
  def apply(t: ParsedTable): TargetsTable = TargetsTable(t.rows.collect { case Right(r) => r })

  val Zero = TargetsTable(Nil)

  implicit val monoid =
    Monoid.instance[TargetsTable](Zero, (a, b) => TargetsTable(a.rows |+| b.rows))
}
