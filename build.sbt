import sbtcrossproject.crossProject
import sbtcrossproject.CrossType

lazy val attoVersion                 = "0.7.2"
lazy val catsVersion                 = "2.1.1"
lazy val kindProjectorVersion        = "0.11.0"
lazy val gspMathVersion              = "0.2.8+16-60a789eb-SNAPSHOT"
lazy val gspCoreVersion              = "0.2.8+20-965c3dc2+20200819-0044-SNAPSHOT"
lazy val catsTestkitScalaTestVersion = "2.0.0"
lazy val monocleVersion              = "2.0.5"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    homepage := Some(url("https://github.com/gemini-hlsw/gpp-catalog")),
    addCompilerPlugin(
      ("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full)
    )
  ) ++ gspPublishSettings
)

skip in publish := true

lazy val catalog = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/catalog"))
  .settings(
    name := "gpp-catalog",
    libraryDependencies ++= Seq(
      // "org.tpolecat"               %%% "atto-core"               % attoVersion,
      "edu.gemini"                 %%% "gsp-core-model" % gspCoreVersion,
      "edu.gemini"                 %%% "gsp-math"       % gspMathVersion,
      "org.typelevel"              %%% "cats-core"      % catsVersion,
      "org.scala-lang.modules"     %%% "scala-xml"      % "2.0.0-M1",
      "com.github.julien-truffaut" %%% "monocle-state"  % monocleVersion
      //   "com.github.julien-truffaut" %%% "monocle-macro"           % monocleVersion,
      //   "org.scala-lang.modules"     %%% "scala-collection-compat" % collCompatVersion
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)
  .jsSettings(
    // libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion
  )

lazy val testkit = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/testkit"))
  .dependsOn(catalog)
  .settings(
    name := "gpp-catalog-testkit",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-testkit"           % catsVersion,
      "org.typelevel" %%% "cats-testkit-scalatest" % catsTestkitScalaTestVersion
      // "com.github.julien-truffaut" %%% "monocle-law"            % monocleVersion,
    )
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)

lazy val tests   = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/tests"))
  .dependsOn(catalog, testkit)
  .settings(
    name := "gpp-catalog-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit"            % "0.7.11" % Test,
      "org.typelevel" %%% "discipline-munit" % "0.2.3"  % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    skip in publish := true
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)
  .jsSettings(scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)))
