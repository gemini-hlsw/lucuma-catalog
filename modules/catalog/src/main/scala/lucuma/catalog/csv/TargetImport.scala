// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package lucuma.catalog.csv

import cats.*
import cats.data.*
import cats.effect.Concurrent
import cats.effect.IO
import cats.parse.Parser
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
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.ProperMotion.AngularVelocityComponent
import lucuma.core.math.RightAscension
import lucuma.core.math.VelocityAxis
import lucuma.core.math.parser.AngleParsers
import lucuma.core.math.parser.EpochParsers
import lucuma.core.model.SiderealTracking
import lucuma.core.model.SiderealTracking.apply
import lucuma.core.model.SourceProfile
import lucuma.core.model.SpectralDefinition
import lucuma.core.model.Target
import lucuma.core.model.UnnormalizedSED
import lucuma.core.parser.MiscParsers
import lucuma.core.parser.TimeParsers
import lucuma.core.syntax.string.*
import org.http4s.Method.*
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client
import org.typelevel.ci.*

import scala.collection.immutable.SortedMap

private case class TargetCsvRow(
  line:  Option[Long],
  name:  NonEmptyString,
  bare:  Boolean,
  ra:    Option[RightAscension],
  dec:   Option[Declination],
  pmRA:  Option[ProperMotion.AngularVelocityComponent[VelocityAxis.RA]],
  pmDec: Option[ProperMotion.AngularVelocityComponent[VelocityAxis.Dec]],
  epoch: Option[Epoch]
)

object TargetImport:

  given CellDecoder[NonEmptyString] =
    CellDecoder.stringDecoder.emap(r =>
      refineV[NonEmpty](r).leftMap(_ => new DecoderError("Empty name"))
    )

  given ParseableHeader[CIString] =
    _.map(x => CIString(x.trim)).asRight

  given decDecoder: CellDecoder[Option[Declination]] =
    CellDecoder.stringDecoder.emap { r =>
      val t = r.trim()
      if (t.isEmpty) Right(None)
      else
        Declination.fromStringSignedDMS
          .getOption(r.trim)
          .map(Some(_))
          .toRight(new DecoderError(s"Invalid Dec value '$r'"))
    }

  given raDecoder: CellDecoder[Option[RightAscension]] =
    CellDecoder.stringDecoder.emap { r =>
      val t = r.trim()
      if (t.isEmpty) Right(None)
      else
        RightAscension.lenientFromStringHMS
          .getOption(r.trim)
          .map(Some(_))
          .toRight(new DecoderError(s"Invalid RA value '$r'"))
    }

  // Move to lucuma-core
  val plainNumberEpoch: Parser[Epoch] =
    TimeParsers.year4.mapFilter(y =>
      if (y.getValue() >= 1900 && y.getValue() <= 3000)
        Epoch.Julian.fromIntMilliyears(y.getValue())
      else None
    )

  val epochParser: Parser[Epoch] =
    EpochParsers.epoch | EpochParsers.epochLenientNoScheme | plainNumberEpoch

  given CellDecoder[Option[Epoch]] =
    CellDecoder.stringDecoder.emap { r =>
      if (r.trim().isEmpty) Right(Epoch.J2000.some)
      else
        epochParser
          .parseAll(r.trim())
          .map(Some(_))
          .leftMap(_ => new DecoderError(s"Invalid epoch value '$r'"))
    }

  given angularVelocityOpt[A]: CellDecoder[Option[ProperMotion.AngularVelocityComponent[A]]] =
    CellDecoder.stringDecoder.emap { r =>
      val t = r.trim()
      if (t.isEmpty) Right(None)
      else
        t.parseBigDecimalOption
          .map(r =>
            ProperMotion.AngularVelocityComponent.milliarcsecondsPerYear
              .reverseGet(r)
              .some
          )
          .toRight(new DecoderError(r))
    }

  extension [A](r: DecoderResult[Option[A]])
    def defaultToNone[B](row: CsvRow[B]): DecoderResult[Option[A]] = r match
      case a @ Right(_)                                          => a
      case Left(d) if d.getMessage().startsWith("unknown field") => Right(None)
      case Left(d)                                               => d.withLine(row.line).asLeft

  given CsvRowDecoder[TargetCsvRow, CIString] =
    (row: CsvRow[CIString]) =>
      for {
        name  <- row.as[NonEmptyString](ci"Name")
        ra    <- row.as[Option[RightAscension]](ci"RAJ2000").defaultToNone(row)
        dec   <- row.as[Option[Declination]](ci"DecJ2000").defaultToNone(row)
        pmRa  <- row
                   .as[Option[ProperMotion.AngularVelocityComponent[VelocityAxis.RA]]](ci"pmRa")
                   .defaultToNone(row)
        pmDec <- row
                   .as[Option[ProperMotion.AngularVelocityComponent[VelocityAxis.Dec]]](ci"pmDec")
                   .defaultToNone(row)
        epoch <- row.as[Option[Epoch]](ci"epoch").defaultToNone(row)
        bare   = ra.isEmpty || dec.isEmpty
      } yield TargetCsvRow(row.line, name, bare, ra, dec, pmRa, pmDec, epoch)

  val DefaultSourceProfile = SourceProfile.Point(
    SpectralDefinition.BandNormalized(
      UnnormalizedSED.StellarLibrary(StellarLibrarySpectrum.O5V),
      SortedMap.empty
    )
  )

  private def tracking(t: TargetCsvRow, ra: RightAscension, dec: Declination): SiderealTracking =
    val base = Coordinates(ra, dec)
    val pm   = (t.pmRA, t.pmDec) match
      case (Some(ra), Some(dec)) => ProperMotion(ra, dec).some
      case (Some(ra), None)      => ProperMotion(ra, ProperMotion.Dec.Zero).some
      case (None, Some(dec))     => ProperMotion(ProperMotion.RA.Zero, dec).some
      case _                     => None
    SiderealTracking(base, t.epoch.getOrElse(Epoch.J2000), pm, none, none)

  private def csv2targetsRows[F[_]: RaiseThrowable]: Pipe[F, String, DecoderResult[TargetCsvRow]] =
    in =>
      in
        .through(lowlevel.rows[F, String]())
        .through(lowlevel.headers[F, CIString])
        .through(lowlevel.attemptDecodeRow[F, CIString, TargetCsvRow])

  def csv2targets[F[_]: RaiseThrowable]
    : Pipe[F, String, EitherNec[ImportProblem, Target.Sidereal]] =
    csv2targetsRows.andThen(
      _.map(t =>
        t.leftMap(e => ImportProblem.CsvParsingError(e.getMessage, e.line))
          .map(t =>
            (t.ra, t.dec)
              .mapN((ra, dec) =>
                Target
                  .Sidereal(name = t.name,
                            tracking = tracking(t, ra, dec),
                            sourceProfile = DefaultSourceProfile,
                            None
                  )
              )
              .getOrElse(
                Target.Sidereal(t.name,
                                tracking = SiderealTracking.const(Coordinates.Zero),
                                sourceProfile = DefaultSourceProfile,
                                None
                )
              )
          )
          .toEitherNec
      )
    )

  def csv2targetsAndLookup[F[_]: Concurrent](
    client: Client[F],
    proxy:  Option[Uri] = None
  ): Pipe[F, String, EitherNec[ImportProblem, Target.Sidereal]] =
    csv2targetsRows.andThen { t =>
      t.evalMap {
        case Left(e)  =>
          ImportProblem
            .CsvParsingError(e.getMessage, e.line)
            .asLeft[Target.Sidereal]
            .toEitherNec[ImportProblem]
            .pure[F]
        case Right(t) =>
          (t.ra, t.dec)
            .mapN((ra, dec) =>
              // If ra/dec are defined just parse
              Target
                .Sidereal(name = t.name,
                          tracking = tracking(t, ra, dec),
                          sourceProfile = DefaultSourceProfile,
                          None
                )
                .rightNec
                .pure[F]
            )
            .getOrElse {
              // If only there is name do a lookup
              val queryUri = CatalogSearch.simbadSearchQuery(QueryByName(t.name, proxy))
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
                // Convert catalog errors to import errors
                .map(
                  _.map(
                    _.leftMap(e => ImportProblem.LookupError(e.foldMap(_.displayValue), t.line))
                      .leftWiden[ImportProblem]
                  )
                )
                .map(imports =>
                  // Fail if there is more than one result
                  val result: EitherNec[ImportProblem, Target.Sidereal] =
                    if (imports.length === 1) imports.head.map(_.target).toEitherNec
                    else
                      ImportProblem
                        .LookupError(s"Multiple or no matches for ${t.name}", t.line)
                        .leftNec
                  result
                )
                // Handle general errors
                .handleError { e =>
                  e.printStackTrace()
                  ImportProblem
                    .LookupError(e.getMessage, t.line)
                    .asLeft[Target.Sidereal]
                    .toEitherNec
                }
            }

      }
    }
