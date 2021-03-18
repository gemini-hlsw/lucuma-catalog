import sbtcrossproject.crossProject
import sbtcrossproject.CrossType

lazy val fs2Version              = "2.5.3"
lazy val fs2DataVersion          = "0.9.0"
lazy val catsVersion             = "2.4.2"
lazy val catsEffectVersion       = "2.3.3"
lazy val kindProjectorVersion    = "0.11.3"
lazy val sttpVersion             = "3.1.9"
lazy val pprintVersion           = "0.6.2"
lazy val lucumaCoreVersion       = "0.7.9"
lazy val monocleVersion          = "2.1.0"
lazy val munitVersion            = "0.7.22"
lazy val munitDisciplineVersion  = "1.0.6"
lazy val munitCatsEffectVersion  = "0.3.0"
lazy val betterMonadicForVersion = "0.3.1"
lazy val refinedVersion          = "0.9.21"
lazy val catsScalacheckVersion   = "0.3.0"
lazy val scalaXmlVersion         = "1.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    homepage := Some(url("https://github.com/gemini-hlsw/lucuma-catalog")),
    addCompilerPlugin(
      ("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full)
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForVersion),
    scalacOptions in Global += "-Ymacro-annotations"
  ) ++ lucumaPublishSettings
)

skip in publish := true

lazy val catalog = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/catalog"))
  .settings(
    name := "lucuma-catalog",
    libraryDependencies ++= Seq(
      "co.fs2"                     %%% "fs2-core"      % fs2Version,
      "org.gnieh"                  %%% "fs2-data-xml"  % fs2DataVersion,
      "edu.gemini"                 %%% "lucuma-core"   % lucumaCoreVersion,
      "org.typelevel"              %%% "cats-core"     % catsVersion,
      "com.github.julien-truffaut" %%% "monocle-core"  % monocleVersion,
      "com.github.julien-truffaut" %%% "monocle-macro" % monocleVersion,
      "com.github.julien-truffaut" %%% "monocle-state" % monocleVersion,
      "eu.timepit"                 %%% "refined"       % refinedVersion,
      "eu.timepit"                 %%% "refined-cats"  % refinedVersion
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(lucumaScalaJsSettings: _*)

lazy val testkit = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/testkit"))
  .dependsOn(catalog)
  .settings(
    name := "lucuma-catalog-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel"     %%% "cats-testkit"       % catsVersion,
      "eu.timepit"        %%% "refined-scalacheck" % refinedVersion,
      "io.chrisdavenport" %%% "cats-scalacheck"    % catsScalacheckVersion
    )
  )
  .jvmConfigure(_.disablePlugins(AutomateHeaderPlugin))
  .jsSettings(lucumaScalaJsSettings: _*)

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("modules/tests"))
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .dependsOn(catalog, testkit)
  .settings(
    name := "lucuma-catalog-tests",
    libraryDependencies ++= Seq(
      "org.typelevel"                 %%% "cats-effect"       % catsEffectVersion      % Test,
      "org.scalameta"                 %%% "munit"             % munitVersion           % Test,
      "org.typelevel"                 %%% "discipline-munit"  % munitDisciplineVersion % Test,
      "org.typelevel"                 %%% "munit-cats-effect" % munitCatsEffectVersion % Test,
      "org.scala-lang.modules"        %%% "scala-xml"         % scalaXmlVersion        % Test,
      "com.softwaremill.sttp.client3" %%% "core"              % sttpVersion,
      "com.lihaoyi"                   %%% "pprint"            % pprintVersion
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    skip in publish := true
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(lucumaScalaJsSettings: _*)
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
