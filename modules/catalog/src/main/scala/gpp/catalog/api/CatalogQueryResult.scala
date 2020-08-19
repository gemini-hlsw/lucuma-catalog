// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gpp.catalog

// import cats._
// import cats.implicits._

/** ParsedTable and ParsedResources contains a list of problems */
// case class ParsedTable(rows: List[Either[CatalogProblem, Target]]) {
//   def containsError: Boolean = rows.exists(_.isLeft)
// }
//
// object ParsedTable        {
//   val Zero = ParsedTable(Nil)
//
//   implicit val monoid =
//     Monoid.instance[ParsedTable](ParsedTable.Zero, (a, b) => ParsedTable(a.rows |+| b.rows))
// }
//
// case class ParsedVoResource(tables: List[ParsedTable]) {
//   def containsError: Boolean = tables.exists(_.containsError)
// }
//
// /** The result of parsing a Catalog Query is a list of targets */
// case class TargetsTable[G[_]](rows: G[Target])

// object TargetsTable {
// // def apply[G[_]: Foldable](t: ParsedTable): TargetsTable[G] =
//   new TargetsTable[G](Foldable[F]. t.rows.collect { case Right(r) => r })
//
// def Zero[G[_]: Alternative] = new TargetsTable[G](Alternative[G].empty)
//
// implicit val monoid =
//   Monoid.instance[TargetsTable](Zero, (a, b) => TargetsTable(a.rows |+| b.rows))
// }

case class CatalogQueryResult[G[_]](targets: TargetsTable, problems: G[CatalogProblem]) {
  // def containsError: Boolean = problems.nonEmpty

  // def filter(query: CatalogQuery): CatalogQueryResult = {
  //   val t = targets.rows.filter(query.filter)
  //   copy(targets = TargetsTable(t))
  // }
}

object CatalogQueryResult {
  // def apply(r: ParsedVoResource): CatalogQueryResult =
  //   CatalogQueryResult(TargetsTable(r.tables.foldMap(identity)),
  //                      r.tables.flatMap(_.rows.collect { case Left(p) => p })
  //   )
  //
  // val Zero = CatalogQueryResult(TargetsTable.Zero, Nil)
  //
  // implicit val monoid = Monoid.instance[CatalogQueryResult](
  //   Zero,
  //   (a, b) => CatalogQueryResult(a.targets |+| b.targets, a.problems |+| b.problems)
  // )
}

case class QueryResult[G[_]](query: CatalogQuery, result: CatalogQueryResult[G])
