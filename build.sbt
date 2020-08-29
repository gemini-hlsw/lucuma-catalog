import sbtcrossproject.crossProject
import sbtcrossproject.CrossType

lazy val attoVersion                 = "0.7.2"
lazy val catsVersion                 = "2.1.1"
lazy val kindProjectorVersion        = "0.11.0"
lazy val lucumaCoreVersion           = "0.3.0+34-ac3c2179-SNAPSHOT"//0.3.0+29-7dcb8b9c-SNAPSHOT"
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
    name := "lucuma-catalog",
    libraryDependencies ++= Seq(
      // "org.tpolecat"               %%% "atto-core"               % attoVersion,
      "edu.gemini"                 %%% "lucuma-core"   % lucumaCoreVersion,
      "org.typelevel"              %%% "cats-core"     % catsVersion,
      "org.scala-lang.modules"     %%% "scala-xml"     % "2.0.0-M1",
      "com.github.julien-truffaut" %%% "monocle-state" % monocleVersion
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
    name := "lucuma-catalog-testkit",
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
    name := "lucuma-catalog-tests",
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
