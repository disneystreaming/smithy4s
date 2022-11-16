import smithy4s.codegen.BuildInfo.smithyVersion

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / organization := "foobar"

lazy val foo = (project in file("foo"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "foobar" %% "foo" % version.value % Smithy4sCompile
    )
  )

lazy val baz = (project in file("baz"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "foobar" %% "bar" % version.value
    )
  )
