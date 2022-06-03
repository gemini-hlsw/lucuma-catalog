resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

val sbtLucumaVersion = "0.8.3"
addSbtPlugin("edu.gemini"         % "sbt-lucuma-lib"         % sbtLucumaVersion)
addSbtPlugin("edu.gemini"         % "sbt-lucuma-sjs-bundler" % sbtLucumaVersion)
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"            % "0.6.3")
addSbtPlugin("ch.epfl.scala"      % "sbt-scalajs-bundler"    % "0.20.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"                % "0.4.3")
