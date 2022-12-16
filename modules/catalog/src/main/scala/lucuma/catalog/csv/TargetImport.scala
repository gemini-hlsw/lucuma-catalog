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
import fs2.data.csv.CellDecoder.doubleDecoder
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import fs2.text
import lucuma.catalog.*
import lucuma.catalog.votable.CatalogAdapter
import lucuma.catalog.votable.CatalogSearch
import lucuma.catalog.votable.QueryByName
import lucuma.core.enums.Band
import lucuma.core.enums.StellarLibrarySpectrum
import lucuma.core.math.BrightnessUnits.*
import lucuma.core.math.Coordinates
import lucuma.core.math.Declination
import lucuma.core.math.Epoch
import lucuma.core.math.ProperMotion
import lucuma.core.math.ProperMotion.AngularVelocity
import lucuma.core.math.RightAscension
import lucuma.core.math.VelocityAxis
import lucuma.core.math.dimensional.*
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
import lucuma.core.util.*
import org.http4s.Method.*
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

import scala.collection.immutable.SortedMap

private case class TargetCsvRow(
  line:            Option[Long],
  name:            NonEmptyString,
  bare:            Boolean,
  ra:              Option[RightAscension],
  dec:             Option[Declination],
  pmRA:            Option[ProperMotion.RA],
  pmDec:           Option[ProperMotion.Dec],
  epoch:           Option[Epoch],
  brightnesses:    Map[Band, BigDecimal],
  integratedUnits: Map[Band, Units Of Brightness[Integrated]],
  surfaceUnits:    Map[Band, Units Of Brightness[Surface]]
) {

  private def brightnessAndUnit[A](
    brightnesses: Map[Band, BigDecimal],
    units:        Map[Band, Units Of Brightness[A]],
    defaultUnit:  Units Of Brightness[A]
  ): List[(Band, Measure[BigDecimal] Of Brightness[A])] =
    brightnesses.map { b =>
      b._1 ->
        units
          .getOrElse(b._1, defaultUnit)
          .withValueTagged(b._2)
    }.toList

  private lazy val integratedBrightness
    : List[(Band, Measure[BigDecimal] Of Brightness[Integrated])] =
    brightnessAndUnit(brightnesses, integratedUnits, VegaMagnitudeIsIntegratedBrightnessUnit.unit)

  private lazy val surfaceBrightness: List[(Band, Measure[BigDecimal] Of Brightness[Surface])] =
    brightnessAndUnit(brightnesses,
                      surfaceUnits,
                      VegaMagnitudePerArcsec2IsSurfaceBrightnessUnit.unit
    )

  val sourceProfile: SourceProfile =
    if (surfaceUnits.nonEmpty)
      SourceProfile.Uniform(
        SpectralDefinition.BandNormalized(
          UnnormalizedSED.StellarLibrary(StellarLibrarySpectrum.O5V).some,
          SortedMap(surfaceBrightness: _*)
        )
      )
    else
      SourceProfile.Point(
        SpectralDefinition.BandNormalized(
          None,
          SortedMap(integratedBrightness: _*)
        )
      )
}

object TargetImport:
  given liftCellDecoder[A: CellDecoder]: CellDecoder[Option[A]] = s =>
    s.nonEmpty.guard[Option].traverse(_ => CellDecoder[A].apply(s))

  given CellDecoder[NonEmptyString] =
    CellDecoder.stringDecoder.emap(r =>
      refineV[NonEmpty](r).leftMap(_ => new DecoderError("Empty name"))
    )

  given ParseableHeader[String] =
    _.map(_.trim).asRight

  given decDecoder: CellDecoder[Declination] =
    CellDecoder.stringDecoder.emap { r =>
      val t = r.trim()
      Declination.fromStringSignedDMS
        .getOption(r.trim)
        .toRight(new DecoderError(s"Invalid Dec value '$r'"))
    }

  given raDecoder: CellDecoder[RightAscension] =
    CellDecoder.stringDecoder.emap { r =>
      RightAscension.lenientFromStringHMS
        .getOption(r.trim)
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

  private def angularVelocityComponentDecoder[T](build: BigDecimal => T): CellDecoder[T] =
    CellDecoder.stringDecoder.emap { r =>
      val t = r.trim()
      t.parseBigDecimalOption
        .map(r => build(r))
        .toRight(new DecoderError(r))
    }

  given pmRADecoder: CellDecoder[ProperMotion.RA] =
    angularVelocityComponentDecoder(ProperMotion.RA.milliarcsecondsPerYear.reverseGet)

  given pmDecDecoder: CellDecoder[ProperMotion.Dec] =
    angularVelocityComponentDecoder(ProperMotion.Dec.milliarcsecondsPerYear.reverseGet)

  def unitAbbv[A](using enumerated: Enumerated[Units Of Brightness[A]]) =
    enumerated.all.map(u => u.abbv -> u).toMap

  // Add some well knwon synonyms
  val integratedUnits: Map[String, Units Of Brightness[Integrated]] =
    unitAbbv[Integrated] ++ Map("Vega" -> VegaMagnitudeIsIntegratedBrightnessUnit.unit,
                                "AB"   -> ABMagnitudeIsIntegratedBrightnessUnit.unit,
                                "Jy"   -> JanskyIsIntegratedBrightnessUnit.unit
    )

  val surfaceUnits: Map[String, Units Of Brightness[Surface]] = unitAbbv[Surface]

  given integratedDecoder: CellDecoder[Units Of Brightness[Integrated]] =
    CellDecoder.stringDecoder.emap(s =>
      integratedUnits.get(s.trim()).toRight(DecoderError(s"Unknown units $s"))
    )

  given surfaceDecoder: CellDecoder[Units Of Brightness[Surface]] =
    CellDecoder.stringDecoder.emap(s =>
      surfaceUnits.get(s.trim()).toRight(DecoderError(s"Unknown units $s"))
    )

  given bdDecoder: CellDecoder[BigDecimal] =
    CellDecoder.stringDecoder.emap(r =>
      r.trim().parseBigDecimalOption.toRight(DecoderError(s"Failed to parse bigdecimal '$r'"))
    )

  extension [A](r: DecoderResult[Option[A]])
    def defaultToNone[B](row: CsvRow[B]): DecoderResult[Option[A]] = r match
      case a @ Right(_)                                          => a
      case Left(d) if d.getMessage().startsWith("unknown field") => Right(None)
      case Left(d)                                               =>
        d.withLine(row.line).asLeft

  def brightnesses(row: CsvRow[String]): Map[Band, BigDecimal] = Band.all
    .foldLeft(List.empty[(Band, Option[BigDecimal])])((l, t) =>
      (t, row.as[Option[BigDecimal]](t.shortName).toOption.flatten) :: l
    )
    .collect { case (b, Some(v)) =>
      (b, v)
    }
    .toMap

  def units[T](using
    CellDecoder[Units Of Brightness[T]]
  )(row: CsvRow[String]): Map[Band, Units Of Brightness[T]] = Band.all
    .foldLeft(List.empty[(Band, Option[Units Of Brightness[T]])])((l, t) =>
      (t,
       row
         .as[Option[Units Of Brightness[T]]](s"${t.shortName}_sys")
         .toOption
         .flatten
      ) :: l
    )
    .collect { case (b, Some(v)) =>
      (b, v)
    }
    .toMap

  def integratedUnits(row: CsvRow[String]) = units[Integrated](row)
  def surfaceUnits(row:    CsvRow[String]) = units[Surface](row)

  given CsvRowDecoder[TargetCsvRow, String] =
    (row: CsvRow[String]) =>
      for {
        name              <- row.as[NonEmptyString]("Name").orElse(row.as[NonEmptyString]("NAME"))
        ra                <- row.as[Option[RightAscension]]("RAJ2000").defaultToNone(row)
        dec               <- row
                               .as[Option[Declination]]("DecJ2000")
                               .orElse(row.as[Option[Declination]]("DECJ2000"))
                               .defaultToNone(row)
        pmRa              <- row
                               .as[Option[ProperMotion.RA]]("pmRa")
                               .defaultToNone(row)
        pmDec             <- row
                               .as[Option[ProperMotion.Dec]]("pmDec")
                               .defaultToNone(row)
        epoch             <- row.as[Option[Epoch]]("epoch").defaultToNone(row)
        rowBrightnesses    = brightnesses(row)
        rowIntegratedUnits = integratedUnits(row)
        rowSurfaceUnits    = surfaceUnits(row)
        _                 <- if (rowIntegratedUnits.nonEmpty && rowSurfaceUnits.nonEmpty)
                               DecoderError("Cannot mix sourface and integrated units").asLeft
                             else ().asRight
        bare               = ra.isEmpty || dec.isEmpty
      } yield TargetCsvRow(row.line,
                           name,
                           bare,
                           ra,
                           dec,
                           pmRa,
                           pmDec,
                           epoch,
                           rowBrightnesses,
                           rowIntegratedUnits,
                           rowSurfaceUnits
      )

  private def tracking(t: TargetCsvRow, ra: RightAscension, dec: Declination): SiderealTracking =
    val base = Coordinates(ra, dec)
    val pm   = (t.pmRA, t.pmDec) match
      case (Some(ra), Some(dec)) => ProperMotion(ra, dec).some
      case (Some(ra), None)      => ProperMotion(ra, ProperMotion.ZeroDecVelocity).some
      case (None, Some(dec))     => ProperMotion(ProperMotion.ZeroRAVelocity, dec).some
      case _                     => None
    SiderealTracking(base, t.epoch.getOrElse(Epoch.J2000), pm, none, none)

  private def csv2targetsRows[F[_]: RaiseThrowable]: Pipe[F, String, DecoderResult[TargetCsvRow]] =
    in =>
      in
        .through(lowlevel.rows[F, String]())
        .through(lowlevel.headers[F, String])
        .through(lowlevel.attemptDecodeRow[F, String, TargetCsvRow])

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
                            sourceProfile = t.sourceProfile,
                            None
                  )
              )
              .getOrElse(
                Target.Sidereal(t.name,
                                tracking = SiderealTracking.const(Coordinates.Zero),
                                sourceProfile = t.sourceProfile,
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
                          sourceProfile = t.sourceProfile,
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
