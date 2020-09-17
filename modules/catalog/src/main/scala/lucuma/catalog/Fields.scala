// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

// import eu.timepit.refined.types.string._

/** Describes a field */
case class FieldId(id: String, ucd: Ucd)

object FieldId {
  // def apply()=???
}
case class FieldDescriptor(id: FieldId, name: String)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap: Map[FieldId, String] = items.map(i => i.field.id -> i.data).toMap
}
