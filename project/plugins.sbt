resolvers ++= Resolver.sonatypeOssRepos("public")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

addSbtPlugin("edu.gemini"         % "sbt-lucuma-lib" % "0.9.1")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"    % "0.6.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh"        % "0.4.3")
