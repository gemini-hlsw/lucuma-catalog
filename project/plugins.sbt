resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

addSbtPlugin("edu.gemini"         % "sbt-lucuma-lib" % "0.9.0-13-55b2257-SNAPSHOT")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"    % "0.6.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"        % "0.4.3")
