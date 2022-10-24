// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.*
import cats.data.*
import cats.effect.Concurrent
import cats.syntax.all.*
import eu.timepit.refined.*
import eu.timepit.refined.api.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.types.string.NonEmptyString
import fs2.*
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import fs2.text
import lucuma.catalog.*
import lucuma.catalog.votable.CatalogAdapter
import lucuma.catalog.votable.CatalogSearch
import lucuma.catalog.votable.QueryByName
import lucuma.core.enums.StellarLibrarySpectrum
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.RightAscension
import lucuma.core.model.SiderealTracking
import lucuma.core.model.SiderealTracking.apply
import lucuma.core.model.SourceProfile
import lucuma.core.model.SpectralDefinition
import lucuma.core.model.Target
import lucuma.core.model.UnnormalizedSED
import org.http4s.Method.*
import org.http4s.Request
import org.http4s.client.Client
import org.typelevel.ci.*

import scala.collection.immutable.SortedMap

case class TargetCsvRow(
  name: NonEmptyString,
  bare: Boolean,
  ra:   Option[RightAscension],
  dec:  Option[Declination]
)

object TargetImport extends AngleParsers:

  given CellDecoder[NonEmptyString] =
    CellDecoder.stringDecoder.emap(r =>
      refineV[NonEmpty](r).leftMap(_ => new DecoderError("Empty name"))
    )

  given ParseableHeader[CIString] =
    _.map(CIString(_)).asRight

  given CellDecoder[Declination] =
    CellDecoder.stringDecoder.emap { r =>
      lenientFromStringDMS
        .getOption(r.trim)
        .liftTo[DecoderResult](new DecoderError("Unknown Dec"))
    }

  given CellDecoder[RightAscension] =
    CellDecoder.stringDecoder.emap { r =>
      lenientFromStringHMS
        .getOption(r.trim)
        .liftTo[DecoderResult](new DecoderError("Cannot parse RA"))
    }

  given CsvRowDecoder[TargetCsvRow, CIString] =
    (row: CsvRow[CIString]) =>
      for {
        name <- row.as[NonEmptyString](ci"Name")
        ra   <- row.as[RightAscension](ci"RAJ2000").map(Some(_)).leftFlatMap(r => Right(None))
        dec  <- row.as[Declination](ci"DecJ2000").map(Some(_)).leftFlatMap(_ => Right(None))
        bare  = ra.isEmpty || dec.isEmpty
      } yield TargetCsvRow(name, bare, ra, dec)

  val DefaultSourceProfile = SourceProfile.Point(
    SpectralDefinition.BandNormalized(
      UnnormalizedSED.StellarLibrary(StellarLibrarySpectrum.O5V),
      SortedMap.empty
    )
  )

  def csv2targets[F[_]: RaiseThrowable]
    : Pipe[F, String, EitherNec[ImportProblem, Target.Sidereal]] =
    in =>
      in
        .through(text.lines)
        .map(s => s.split(",").map(_.trim()).mkString("", ",", "\n"))
        .through(lowlevel.rows[F, String]())
        .through(lowlevel.headers[F, CIString])
        .through(lowlevel.decodeRow[F, CIString, TargetCsvRow])
        .map(t =>
          Target
            .Sidereal(name = t.name,
                      tracking = SiderealTracking.const(
                        (t.ra, t.dec).mapN(Coordinates.apply).getOrElse(Coordinates.Zero)
                      ),
                      sourceProfile = DefaultSourceProfile,
                      None
            )
            .asRight
        )

  def csv2targetsAndLookup[F[_]: Concurrent](
    client: Client[F]
  ): Pipe[F, String, EitherNec[ImportProblem, Target.Sidereal]] =
    in =>
      in
        .through(text.lines)
        .map(s => s.split(",").map(_.trim()).mkString("", ",", "\n"))
        .through(lowlevel.rows[F, String]())
        .through(lowlevel.headers[F, CIString])
        .through(lowlevel.decodeRow[F, CIString, TargetCsvRow])
        .evalMap(_.toSiderealTarget[F](client))

  extension (t: TargetCsvRow)
    def toSiderealTarget[F[_]: Concurrent](
      client: Client[F]
    ): F[EitherNec[ImportProblem, Target.Sidereal]] =
      if (!t.bare) {
        (t.ra, t.dec)
          .mapN((ra, dec) =>
            Target.Sidereal(
              name = t.name,
              tracking = SiderealTracking.const(Coordinates(ra, dec)),
              sourceProfile = DefaultSourceProfile,
              None
            )
          )
          .map(_.rightNec)
          .getOrElse(ImportProblem.MissingCoordinates.leftNec)
          .pure[F]
      } else {
        val queryUri = CatalogSearch.simbadSearchQuery(QueryByName(t.name))
        val request  = Request[F](GET, queryUri)
        client
          .stream(request)
          .flatMap(
            _.body
              .through(text.utf8.decode)
              .through(CatalogSearch.siderealTargets[F](CatalogAdapter.Simbad))
          )
          .compile
          .toList
          .map(
            _.map(
              _.leftMap(e => ImportProblem.LookupError(e.foldMap(_.displayValue)))
                .leftWiden[ImportProblem]
            )
          )
          .map(imports =>
            val result: EitherNec[ImportProblem, Target.Sidereal] =
              if (imports.length === 1) imports.head.map(_.target).toEitherNec
              else
                ImportProblem
                  .LookupError(s"Multiple matches for ${t.name}")
                  .leftNec
            result
          )
      }
