// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.data.ValidatedNel
import cats.implicits._
import lucuma.core.model.Target

/** ParsedTable and ParsedResources contains a list of problems */
case class ParsedTable[F[_]](rows: ValidatedNel[CatalogProblem, F[Target]]) {
  // def containsError: Boolean = rows.exists(_.isLeft)
}

object ParsedTable {
  // val Zero = ParsedTable(Nil)
  //
  // implicit val monoid =
  //   Monoid.instance[ParsedTable](ParsedTable.Zero, (a, b) => ParsedTable(a.rows |+| b.rows))
}

case class ParsedVoResource[F[_]](tables: F[ParsedTable[F]]) {
  // def containsError: Boolean = tables.exists(_.containsError)
}

/** The result of parsing a Catalog Query is a list of targets */
case class TargetsTable(rows: List[Target])

object TargetsTable {
  // def apply(t: ParsedTable): TargetsTable = TargetsTable(t.rows.collect { case Right(r) => r })

  val Zero = TargetsTable(Nil)

  implicit val monoid =
    Monoid.instance[TargetsTable](Zero, (a, b) => TargetsTable(a.rows |+| b.rows))
}
