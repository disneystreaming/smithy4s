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
    // Bar refers to foo explicitly in its ivy deps, and upon publishing,
    // this information is stored in the manifest of bar's jar, for downstream
    // consumption
    libraryDependencies ++= Seq(
      "foobar" %% "foo" % version.value % Smithy4sCompile
    )
  )

lazy val baz = (project in file("baz"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    // baz depend on bar, and an assumption is made that baz may depend on the same smithy models
    // that bar depended on for its own codegen. Therefore, these are retrieved from bar's manifest,
    // resolved and added to the list of jars to seek smithy models from during code generation
    libraryDependencies ++= Seq(
      "foobar" %% "bar" % version.value
    )
  )
