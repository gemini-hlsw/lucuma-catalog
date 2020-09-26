# lucuma-catalog

Scala jvm/js library to read votable catalogs.

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

The function is an `fs2` `Pipe` that will take a `Stream[F, String]` attempt to first parse its
xml content and then produce a stream of `Targets`. This means we don't need to wait for the whole
document to be parsed.

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

