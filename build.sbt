import sbtcrossproject.crossProject
import sbtcrossproject.CrossType

lazy val attoVersion             = "0.7.2"
lazy val fs2Version              = "2.4.1"
lazy val catsVersion             = "2.2.0"
lazy val catsEffectVersion       = "2.2.0"
lazy val kindProjectorVersion    = "0.11.0"
lazy val sttpVersion             = "3.0.0-RC2"
lazy val lucumaCoreVersion       = "0.4.5+3-1054e30b-SNAPSHOT"
lazy val monocleVersion          = "2.1.0"
lazy val munitVersion            = "0.7.12"
lazy val munitDisciplineVersion  = "0.3.0"
lazy val betterMonadicForVersion = "0.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    homepage := Some(url("https://github.com/gemini-hlsw/lucuma-catalog")),
    addCompilerPlugin(
      ("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full)
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForVersion)
  ) ++ gspPublishSettings
)

skip in publish := true

lazy val catalog      = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/catalog"))
  .settings(
    name := "lucuma-catalog",
    libraryDependencies ++= Seq(
      // "org.tpolecat"               %%% "atto-core"               % attoVersion,
      "co.fs2"                       %%% "fs2-core"      % fs2Version,
      "edu.gemini"                   %%% "lucuma-core"   % lucumaCoreVersion,
      "org.typelevel"                %%% "cats-core"     % catsVersion,
      "org.typelevel"                %%% "cats-effect"   % catsEffectVersion,
      "org.scala-lang.modules"       %%% "scala-xml"     % "2.0.0-M1",
      "com.github.julien-truffaut"   %%% "monocle-state" % monocleVersion,
      "com.softwaremill.sttp.client" %%% "core"          % sttpVersion
      //   "com.github.julien-truffaut" %%% "monocle-macro"           % monocleVersion,
      //   "org.scala-lang.modules"     %%% "scala-collection-compat" % collCompatVersion
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)
  .jsSettings(
    // libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion
  )
  .dependsOn(fs2_data_xml)

lazy val fs2_data_xml = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/fs2-data-xml"))
  .settings(
    name := "fs2-data-xml",
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % fs2Version
      // "org.typelevel" %%% "cats-testkit-scalatest" % catsTestkitScalaTestVersion
      // "com.github.julien-truffaut" %%% "monocle-law"            % monocleVersion,
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)

lazy val testkit      = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/testkit"))
  .dependsOn(catalog)
  .settings(
    name := "lucuma-catalog-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-testkit" % catsVersion
      // "org.typelevel" %%% "cats-testkit-scalatest" % catsTestkitScalaTestVersion
      // "com.github.julien-truffaut" %%% "monocle-law"            % monocleVersion,
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)

lazy val tests        = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/tests"))
  .dependsOn(catalog, testkit)
  .settings(
    name := "lucuma-catalog-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit"            % munitVersion           % Test,
      "org.typelevel" %%% "discipline-munit" % munitDisciplineVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    skip in publish := true
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
