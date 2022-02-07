resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("edu.gemini"       % "sbt-lucuma-lib"      % "0.6-7c8c046-SNAPSHOT")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"         % "0.6.2")
addSbtPlugin("ch.epfl.scala"    % "sbt-scalajs-bundler" % "0.20.0")
