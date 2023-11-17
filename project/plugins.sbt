// format: off
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.11.1")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % "1.14.0")
addSbtPlugin("com.github.sbt"       % "sbt-pgp"                       % "2.2.1")
addSbtPlugin("com.github.sbt"       % "sbt-dynver"                    % "5.0.1")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.10.0")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.4")
// addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.9.0")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.3.5")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"                 % "0.11.0")
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"             % "0.9.1")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"                       % "0.4.6")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"                    % "5.9.0")
addSbtPlugin("org.scala-native"     % "sbt-scala-native"              % "0.4.16")
addSbtPlugin("com.github.sbt"       % "sbt-git"                       % "2.0.1")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"               % "1.1.3")
addSbtPlugin("ch.epfl.scala"        % "sbt-bloop"                     % "1.5.11")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "os-lib" % "0.8.1",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.24.4"
)

addDependencyTreePlugin
