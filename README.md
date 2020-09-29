# lucuma-catalog

Scala JVM/JS library to read votable catalogs.

## VOTable

[VOTable](http://www.ivoa.net/documents/VOTable/20191021/REC-VOTable-1.4-20191021.html) is an xml
format to deliver astronomical table based data.

It has a heritage based on FITS, having a preamble of metadata describing the table rows and the
data in a table format

`lucuma-catalog` provides functions to read the xml and convert it to `lucuma-core` `Targets`.

## Usage

`lucuma-catalog` is centered around a single function.

```scala
  def targets(catalog: CatalogName): Pipe[F, String, ValidatedNec[CatalogProblem, Target]]
```

The function is an `fs2` `Pipe` that will take a `Stream[F, String]`, attempt to first parse its
xml content and then produce a stream of `Targets`. This means we don't need to wait for the whole
document to be parsed to start getting results.

Note that we fail at the level of targets, this would allow to keep getting results even if one particular
target cannot be read for any reason

Though `VOTable` is a formal standard different catalogs can use different metadata and thus we
need to tell the `targets` function what specific catalog we are using.

## Example

```scala
import lucuma.catalog._

val file    = new File("votabl.xml")

Blocker[IO].use { blocker =>
  io.file
    .readAll[IO](Paths.get(file.toURI), blocker, 1024)
    .through(text.utf8Decode)
    .through(targets(CatalogName.Simbad))
    .compile
    .lastOrError
    .unsafeRunSync()
}
```
Examples for [JVM](modules/tests/jvm/src/main/scala/lucuma/catalog/SimbadQuerySample.scala) and [JS](modules/tests/js/src/main/scala/lucuma/catalog/SimbadQuerySample.scala) are provided that will query [Simbad](http://simbad.u-strasbg.fr/simbad/) using [sttp](https://github.com/softwaremill/sttp) as http client

## fs2-data-xml

To stream parse xml we are using [fs2-data-xml](https://github.com/satabin/fs2-data), however
the project is not yet available for both JVM/JS.

This is temporarily solved internalizing the code for the specific module and cross compile it here.

This won't not needed once https://github.com/satabin/fs2-data/issues/58 is solved
