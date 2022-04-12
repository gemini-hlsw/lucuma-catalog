// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats.syntax.all._

sealed trait ADQLGaiaQuery {

  val gaia = CatalogAdapter.Gaia

  /**
   * Builds an adql query for gaia taking input from the adapter and the query itself
   */
  def adql(cs: QueryByADQL)(implicit ci: ADQLInterpreter): String = {
    //
    val fields         = gaia.allFields.map(_.id.value.toLowerCase).mkString(",")
    val extraFields    = ci.extraFields(cs.base)
    val extraFieldsStr =
      if (extraFields.isEmpty) "" else extraFields.mkString(",", ",", "")
    val shapeAdql      = cs.adqlGeom
    val orderBy        = ci.orderBy.foldMap(s => s"ORDER BY $s")

    val query =
      f"""|SELECT TOP ${ci.MaxCount} $fields $extraFieldsStr
        |     FROM gaiadr2.gaia_source
        |     WHERE CONTAINS(POINT('ICRS',${gaia.raField.id},${gaia.decField.id}),$shapeAdql)=1
        |     $orderBy
      """.stripMargin
    query
  }

}
object ADQLGaiaQuery extends ADQLGaiaQuery
