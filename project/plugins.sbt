// format: off
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.10.4")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % "1.11.0")
addSbtPlugin("com.github.sbt"       % "sbt-pgp"                       % "2.2.0")
addSbtPlugin("com.dwijnand"         % "sbt-dynver"                    % "4.1.1")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.9.14")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.4")
// addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.9.0")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.3.5")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"                 % "0.11.0")
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"             % "0.9.0")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"                       % "0.4.3")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"                    % "5.9.0")
addSbtPlugin("org.scala-native"     % "sbt-scala-native"              % "0.4.8")
addSbtPlugin("com.github.sbt"       % "sbt-git"                       % "2.0.0")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"               % "1.1.1")

libraryDependencies ++= Seq("com.lihaoyi" %% "os-lib" % "0.8.1")
// See https://stackoverflow.com/questions/74335368/scala-sbt-version-dependency-binary-compatibility-error-scala-xml
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addDependencyTreePlugin
