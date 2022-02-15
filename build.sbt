lazy val fs2Version              = "3.2.4"
lazy val fs2DataVersion          = "1.3.1"
lazy val catsVersion             = "2.7.0"
lazy val catsEffectVersion       = "3.0.1"
lazy val kindProjectorVersion    = "0.13.2"
lazy val sttpVersion             = "3.4.2"
lazy val pprintVersion           = "0.7.1"
lazy val lucumaCoreVersion       = "0.24.0"
lazy val monocleVersion          = "3.1.0"
lazy val munitVersion            = "0.7.29"
lazy val munitDisciplineVersion  = "1.0.9"
lazy val munitCatsEffectVersion  = "1.0.7"
lazy val betterMonadicForVersion = "0.3.1"
lazy val refinedVersion          = "0.9.28"
lazy val catsScalacheckVersion   = "0.3.1"
lazy val scalaXmlVersion         = "2.0.1"

Global / onChangedBuildSource   := ReloadOnSourceChanges
Global / scalacOptions += "-Ymacro-annotations"

ThisBuild / tlBaseVersion       := "0.10"
ThisBuild / tlCiReleaseBranches := Seq("master")

lazy val root = tlCrossRootProject.aggregate(catalog, testkit, tests)

lazy val catalog = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/catalog"))
  .settings(
    name := "lucuma-catalog",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"      % fs2Version,
      "org.gnieh"     %%% "fs2-data-xml"  % fs2DataVersion,
      "edu.gemini"    %%% "lucuma-core"   % lucumaCoreVersion,
      "org.typelevel" %%% "cats-core"     % catsVersion,
      "dev.optics"    %%% "monocle-core"  % monocleVersion,
      "dev.optics"    %%% "monocle-macro" % monocleVersion,
      "dev.optics"    %%% "monocle-state" % monocleVersion,
      "eu.timepit"    %%% "refined"       % refinedVersion,
      "eu.timepit"    %%% "refined-cats"  % refinedVersion
    ),
    scalacOptions ~= (_.filterNot(Set("-Vtype-diffs")))
  )

lazy val testkit = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/testkit"))
  .dependsOn(catalog)
  .settings(
    name := "lucuma-catalog-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel"     %%% "cats-testkit"        % catsVersion,
      "edu.gemini"        %%% "lucuma-core-testkit" % lucumaCoreVersion,
      "eu.timepit"        %%% "refined-scalacheck"  % refinedVersion,
      "io.chrisdavenport" %%% "cats-scalacheck"     % catsScalacheckVersion
    )
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/tests"))
  .enablePlugins(NoPublishPlugin)
  .jsEnablePlugins(ScalaJSBundlerPlugin)
  .dependsOn(catalog, testkit)
  .settings(
    name := "lucuma-catalog-tests",
    libraryDependencies ++= Seq(
      "org.typelevel"                 %%% "cats-effect"         % catsEffectVersion      % Test,
      "org.scalameta"                 %%% "munit"               % munitVersion           % Test,
      "org.typelevel"                 %%% "discipline-munit"    % munitDisciplineVersion % Test,
      "org.typelevel"                 %%% "munit-cats-effect-3" % munitCatsEffectVersion % Test,
      "org.scala-lang.modules"        %%% "scala-xml"           % scalaXmlVersion        % Test,
      "com.softwaremill.sttp.client3" %%% "core"                % sttpVersion,
      "com.softwaremill.sttp.client3" %%% "cats"                % sttpVersion,
      "com.lihaoyi"                   %%% "pprint"              % pprintVersion
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    Compile / npmDependencies ++= Seq(
      "node-fetch"               -> "2.6.1",
      "abortcontroller-polyfill" -> "1.5.0",
      "fetch-headers"            -> "2.0.0"
    ),
    scalacOptions ~= (_.filterNot(Set("-Wdead-code"))),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % sttpVersion,
      "co.fs2"                        %% "fs2-io"                        % fs2Version
    )
  )
