// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog

import cats._
import cats.implicits._
import scala.util.matching.Regex

case class UcdWord(token: String)
case class Ucd(tokens: List[UcdWord]) {
  def includes(ucd: UcdWord): Boolean = tokens.contains(ucd)
  def matches(r:    Regex): Boolean   = tokens.exists(t => r.findFirstIn(t.token).isDefined)
  override def toString = tokens.map(_.token).mkString(", ")
}

object Ucd {
  def parseUcd(v: String): Ucd =
    Ucd(v.split(";").filter(_.nonEmpty).map(_.toLowerCase).map(UcdWord).toList)

  def apply(ucd: String): Ucd = parseUcd(ucd)

  implicit val eqUcd: Eq[Ucd] = Eq.fromUniversalEquals
}
