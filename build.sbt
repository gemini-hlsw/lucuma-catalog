import sbtcrossproject.crossProject
import sbtcrossproject.CrossType

lazy val attoVersion                 = "0.7.2"
lazy val catsVersion                 = "2.1.1"
lazy val kindProjectorVersion        = "0.10.3"
lazy val gspMathVersion              = "0.1.17"
lazy val gspCoreVersion              = "0.1.8"
lazy val catsTestkitScalaTestVersion = "1.0.1"

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    homepage := Some(url("https://github.com/gemini-hlsw/gpp-catalog"))
    // addCompilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorVersion),
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
      "edu.gemini"             %%% "gsp-core-model" % gspCoreVersion,
      "edu.gemini"             %%% "gsp-math"       % gspMathVersion,
      "org.typelevel"          %%% "cats-core"      % catsVersion,
      "org.scala-lang.modules" %%% "scala-xml"      % "2.0.0-M1"
      //   "com.github.julien-truffaut" %%% "monocle-core"            % monocleVersion,
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
  .crossType(CrossType.Full)
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
  .crossType(CrossType.Full)
  .in(file("modules/tests"))
  .dependsOn(catalog, testkit)
  .settings(
    name := "gpp-catalog-tests",
    skip in publish := true
  )
  .jvmConfigure(_.enablePlugins(AutomateHeaderPlugin))
  .jsSettings(gspScalaJsSettings: _*)
