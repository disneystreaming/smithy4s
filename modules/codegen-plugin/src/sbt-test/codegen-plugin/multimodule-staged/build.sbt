import smithy4s.codegen.BuildInfo.smithyVersion

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / organization := "foobar"

lazy val foo = (project in file("foo"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    // foo refers to smithy-aws-traits explicitly as a code-gen only dep, and upon publishing,
    // this information is stored in the manifest of bar's jar, for downstream consumption
    smithy4sAllowedNamespaces := List("aws.api", "foo"),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "software.amazon.smithy" % "smithy-aws-traits" % smithy4s.codegen.BuildInfo.smithyVersion % Smithy4s
    )
  )

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    // bar depend on foo as a library, and an assumption is made that bar may depend on the same smithy models
    // that foo depended on for its own codegen. Therefore, these are retrieved from foo's manifest,
    // resolved and added to the list of jars to seek smithy models from during code generation
    libraryDependencies ++= Seq(
      "foobar" %% "foo" % version.value
    )
  )
