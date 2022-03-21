// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.data.NonEmptyChain
import cats.data.ValidatedNec
import cats.implicits._
import eu.timepit.refined.cats._
import eu.timepit.refined.cats.syntax._
import eu.timepit.refined.types.string._
import lucuma.catalog.CatalogProblem._
import monocle.macros.Lenses

/** Describes a field */
@Lenses
case class FieldId(id: NonEmptyString, ucd: Option[Ucd])

object FieldId {
  def apply(id: String, ucd: Ucd): ValidatedNec[CatalogProblem, FieldId] =
    NonEmptyString
      .validateNec(id)
      .bimap(_ => NonEmptyChain.one(InvalidFieldId(id)).widen[CatalogProblem],
             FieldId(_, Some(ucd))
      )

  def unsafeFrom(id: String, ucd: Ucd): FieldId =
    apply(id, ucd).getOrElse(sys.error(s"Invalid field id $id"))

  def unsafeFrom(id: String, ucd: String): FieldId =
    Ucd(ucd).andThen(apply(id, _)).getOrElse(sys.error(s"Invalid field id $id"))

  implicit val eqFieldId: Eq[FieldId] = Eq.by(x => (x.id, x.ucd))
}

@Lenses
case class FieldDescriptor(id: FieldId, name: String)

object FieldDescriptor {
  implicit val eqFieldDescriptor: Eq[FieldDescriptor] = Eq.by(x => (x.id, x.name))
}

@Lenses
case class TableRowItem(field: FieldDescriptor, data: String)

@Lenses
case class TableRow(items: List[TableRowItem]) {
  def itemsMap: Map[FieldId, String] = items.map(i => i.field.id -> i.data).toMap
}
