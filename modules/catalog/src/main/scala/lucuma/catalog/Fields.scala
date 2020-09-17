// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.implicits._
import cats.data.ValidatedNec
import cats.data.NonEmptyChain
import eu.timepit.refined.cats.syntax._
import eu.timepit.refined.types.string._

/** Describes a field */
case class FieldId(id: NonEmptyString, ucd: Ucd)

object FieldId {
  def apply(id: String, ucd: Ucd): ValidatedNec[CatalogProblem, FieldId] =
    NonEmptyString
      .validateNec(id)
      .bimap(_ => NonEmptyChain.one(InvalidFieldId(id)).widen[CatalogProblem], FieldId(_, ucd))

  def unsafeFrom(id: String, ucd: Ucd): FieldId =
    apply(id, ucd).getOrElse(sys.error(s"Invalid field id $id"))
}

case class FieldDescriptor(id: FieldId, name: String)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap: Map[FieldId, String] = items.map(i => i.field.id -> i.data).toMap
}
