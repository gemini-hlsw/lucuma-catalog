// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog

import cats._
import cats.implicits._
import gem.Target

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
