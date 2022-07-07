// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.data._
import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string._
import lucuma.catalog.CatalogProblem._

/** Describes a field */
case class FieldId(id: NonEmptyString, ucd: Option[Ucd])

object FieldId {
  def apply(id: String, ucd: Ucd): EitherNec[CatalogProblem, FieldId] =
    refineV[NonEmpty](id)
      .bimap(_ => NonEmptyChain.one(InvalidFieldId(id)).widen[CatalogProblem],
             FieldId(_, Some(ucd))
      )

  def unsafeFrom(id: String, ucd: Ucd): FieldId =
    apply(id, ucd).getOrElse(sys.error(s"Invalid field id $id"))

  def unsafeFrom(id: String): FieldId =
    refineV[NonEmpty](id)
      .bimap(_ => NonEmptyChain.one(InvalidFieldId(id)).widen[CatalogProblem], FieldId(_, None))
      .getOrElse(sys.error(s"Invalid field id $id"))

  def unsafeFrom(id: String, ucd: String): FieldId =
    Ucd(ucd).flatMap(apply(id, _)).getOrElse(sys.error(s"Invalid field id $id"))

  implicit val eqFieldId: Eq[FieldId] = Eq.by(x => (x.id, x.ucd))
}

case class TableRowItem(field: FieldId, data: String)

case class TableRow(items: List[TableRowItem]) {
  def itemsMap: Map[FieldId, String] = items.map(i => i.field -> i.data).toMap
}
