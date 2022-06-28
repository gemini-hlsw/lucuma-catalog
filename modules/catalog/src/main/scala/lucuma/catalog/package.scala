// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma

import cats.data._
import cats.syntax.all._
import eu.timepit.refined._
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import lucuma.catalog.CatalogProblem.FieldValueProblem
import lucuma.core.syntax.string._

package object catalog {
  def refineMV[A](s: String): NonEmptyString =
    refineV[NonEmpty](s).getOrElse(sys.error("Empty string"))

  def parseDoubleValue(
    ucd: Option[Ucd],
    s:   String
  ): EitherNec[CatalogProblem, Double] =
    Either
      .fromOption(s.parseDoubleOption, FieldValueProblem(ucd, s))
      .toEitherNec

  def parseBigDecimalValue(
    ucd: Option[Ucd],
    s:   String
  ): EitherNec[CatalogProblem, BigDecimal] =
    Either
      .fromOption(s.parseBigDecimalOption, FieldValueProblem(ucd, s))
      .toEitherNec
}
