lazy val fs2Version                 = "3.10.2"
lazy val fs2DataVersion             = "1.11.0"
lazy val catsVersion                = "2.12.0"
lazy val catsEffectVersion          = "3.5.4"
lazy val kindProjectorVersion       = "0.13.2"
lazy val pprintVersion              = "0.9.0"
lazy val lucumaCoreVersion          = "0.103.0"
lazy val lucumaRefinedVersion       = "0.1.3"
lazy val monocleVersion             = "3.3.0"
lazy val munitVersion               = "1.0.0"
lazy val munitDisciplineVersion     = "2.0.0"
lazy val munitCatsEffectVersion     = "1.0.7"
lazy val betterMonadicForVersion    = "0.3.1"
lazy val refinedVersion             = "0.11.2"
lazy val catsScalacheckVersion      = "0.3.2"
lazy val scalaXmlVersion            = "2.3.0"
lazy val http4sVersion              = "0.23.27"
lazy val http4sJdkHttpClientVersion = "0.9.1"
lazy val http4sDomVersion           = "0.2.11"
lazy val refinedAlgebraVersion      = "0.1.1"
lazy val catsTimeVersion            = "0.5.1"
lazy val catsParseVersion           = "1.0.0"
lazy val kittensVersion             = "3.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / tlBaseVersion       := "0.47"
ThisBuild / tlCiReleaseBranches := Seq("master", "scala3")

ThisBuild / scalaVersion       := "3.4.2"
ThisBuild / crossScalaVersions := Seq("3.4.2")
ThisBuild / scalacOptions ++= Seq(
  "-language:implicitConversions"
)

lazy val root = tlCrossRootProject.aggregate(catalog, ags, testkit, tests)

lazy val catalog = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/catalog"))
  .settings(
    name := "lucuma-catalog",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"             % fs2Version,
      "org.gnieh"     %%% "fs2-data-xml"         % fs2DataVersion,
      "org.gnieh"     %%% "fs2-data-csv"         % fs2DataVersion,
      "org.gnieh"     %%% "fs2-data-csv-generic" % fs2DataVersion,
      "edu.gemini"    %%% "lucuma-core"          % lucumaCoreVersion,
      "edu.gemini"    %%% "lucuma-refined"       % lucumaRefinedVersion,
      "org.typelevel" %%% "cats-core"            % catsVersion,
      "dev.optics"    %%% "monocle-core"         % monocleVersion,
      "dev.optics"    %%% "monocle-state"        % monocleVersion,
      "eu.timepit"    %%% "refined"              % refinedVersion,
      "eu.timepit"    %%% "refined-cats"         % refinedVersion,
      "org.http4s"    %%% "http4s-core"          % http4sVersion,
      "org.http4s"    %%% "http4s-client"        % http4sVersion,
      "edu.gemini"    %%% "refined-algebra"      % refinedAlgebraVersion,
      "org.typelevel" %%% "cats-parse"           % catsParseVersion,
      "org.typelevel" %%% "kittens"              % kittensVersion
    ),
    scalacOptions ~= (_.filterNot(Set("-Vtype-diffs")))
  )

lazy val ags = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/ags"))
  .settings(
    name := "lucuma-ags",
    libraryDependencies ++= Seq(
      "edu.gemini"    %%% "lucuma-core"    % lucumaCoreVersion,
      "edu.gemini"    %%% "lucuma-refined" % lucumaRefinedVersion,
      "org.typelevel" %%% "cats-core"      % catsVersion
    ),
    scalacOptions ~= (_.filterNot(Set("-Vtype-diffs")))
  )
  .dependsOn(catalog)

lazy val testkit = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/testkit"))
  .dependsOn(catalog, ags)
  .settings(
    name := "lucuma-catalog-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel"          %%% "cats-testkit"        % catsVersion,
      "edu.gemini"             %%% "lucuma-core-testkit" % lucumaCoreVersion,
      "eu.timepit"             %%% "refined-scalacheck"  % refinedVersion,
      "org.scala-lang.modules" %%% "scala-xml"           % scalaXmlVersion,
      "io.chrisdavenport"      %%% "cats-scalacheck"     % catsScalacheckVersion
    )
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/tests"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(catalog, testkit, ags)
  .settings(
    name := "lucuma-catalog-tests",
    libraryDependencies ++= Seq(
      "org.typelevel"          %%% "cats-effect"         % catsEffectVersion      % Test,
      "org.scalameta"          %%% "munit"               % munitVersion           % Test,
      "org.typelevel"          %%% "discipline-munit"    % munitDisciplineVersion % Test,
      "org.typelevel"          %%% "munit-cats-effect-3" % munitCatsEffectVersion % Test,
      "org.scala-lang.modules" %%% "scala-xml"           % scalaXmlVersion        % Test,
      "org.http4s"             %%% "http4s-core"         % http4sVersion,
      "com.lihaoyi"            %%% "pprint"              % pprintVersion,
      "org.typelevel"          %%% "cats-time"           % catsTimeVersion
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    scalacOptions ~= (_.filterNot(Set("-Wdead-code"))),
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dom" % http4sDomVersion
    ),
    jsEnv                           := {
      import org.scalajs.jsenv.nodejs.NodeJSEnv
      new NodeJSEnv(NodeJSEnv.Config().withArgs(List("--experimental-fetch")))
    }
  )
  .jsConfigure(_.enablePlugins(BundleMonPlugin))
  .jvmSettings(
    libraryDependencies ++= Seq(
      "co.fs2"     %% "fs2-io"                 % fs2Version,
      "org.http4s" %% "http4s-jdk-http-client" % http4sJdkHttpClientVersion
    )
  )

lazy val benchmarks = project
  .in(file("modules/benchmarks"))
  .dependsOn(catalog.jvm, tests.jvm % "compile->compile;test->test")
  .settings(name := "lucuma-benchmarks")
  .enablePlugins(NoPublishPlugin, JmhPlugin)
