resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

val sbtLucumaVersion = "0.8.5"
addSbtPlugin("edu.gemini"         % "sbt-lucuma-lib" % sbtLucumaVersion)
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"    % "0.6.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"        % "0.4.3")
