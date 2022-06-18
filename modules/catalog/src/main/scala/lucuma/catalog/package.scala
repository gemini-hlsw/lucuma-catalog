// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma

import cats.data._
import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import lucuma.core.math.refined.*
import lucuma.catalog.CatalogProblem.FieldValueProblem

package object catalog {
  // Move to lucuma-core
  inline given Predicate[String, NonEmpty] with
    transparent inline def isValid(inline t: String): Boolean = t.nonEmpty

  def parseDoubleValue(
    ucd: Option[Ucd],
    s:   String
  ): EitherNec[CatalogProblem, Double] =
    Either
      .catchNonFatal(s.toDouble)
      .leftMap(_ => FieldValueProblem(ucd, s))
      .toEitherNec

}
