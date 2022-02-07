// format: off
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.9.34")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % "1.8.0")
addSbtPlugin("com.github.sbt"       % "sbt-pgp"                       % "2.1.2")
addSbtPlugin("com.dwijnand"         % "sbt-dynver"                    % "4.1.1")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.9.10")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.4")
// addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.9.0")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.2.24")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"                 % "0.10.0")
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"             % "0.9.0")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"                       % "0.4.3")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"                    % "5.6.5")

libraryDependencies ++= Seq("com.lihaoyi" %% "os-lib" % "0.8.1")

addDependencyTreePlugin
