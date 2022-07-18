resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

val sbtLucumaVersion = "0.9.0"
addSbtPlugin("edu.gemini"         % "sbt-lucuma-lib" % sbtLucumaVersion)
addSbtPlugin("com.armanbilge"     % "sbt-bundlemon"  % "0.1.0-M1")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"    % "0.6.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"        % "0.4.3")
