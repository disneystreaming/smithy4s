// format: off
addSbtPlugin("ch.epfl.scala"        % "sbt-scalafix"                  % "0.14.2")
addSbtPlugin("org.scala-js"         % "sbt-scalajs"                   % "1.18.2")
addSbtPlugin("com.github.sbt"       % "sbt-pgp"                       % "2.3.1")
addSbtPlugin("com.github.sbt"       % "sbt-dynver"                    % "5.1.0")
addSbtPlugin("org.xerial.sbt"       % "sbt-sonatype"                  % "3.12.2")
addSbtPlugin("org.scalameta"        % "sbt-scalafmt"                  % "2.4.4")
// addSbtPlugin("org.scoverage"        % "sbt-scoverage"                 % "1.9.0")
addSbtPlugin("org.scalameta"        % "sbt-mdoc"                      % "2.6.5")
addSbtPlugin("com.eed3si9n"         % "sbt-buildinfo"                 % "0.13.1")
addSbtPlugin("com.eed3si9n"         % "sbt-projectmatrix"             % "0.10.1")
addSbtPlugin("pl.project13.scala"   % "sbt-jmh"                       % "0.4.7")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"                    % "5.10.0")
addSbtPlugin("org.scala-native"     % "sbt-scala-native"              % "0.4.17")
addSbtPlugin("com.github.sbt"       % "sbt-git"                       % "2.1.0")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin"               % "1.1.4")
addSbtPlugin("ch.epfl.scala"        % "sbt-bloop"                     % "2.0.9")
addSbtPlugin("com.thesamet"         % "sbt-protoc"                    % "1.0.7")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "os-lib" % "0.11.4",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.30.2",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.15"
)

addDependencyTreePlugin
