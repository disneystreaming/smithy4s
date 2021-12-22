lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.6",
    smithy4sInputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_input",
    smithy4sOutputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_output",
    smithy4sCodegenDependencies in Compile := List(
      "software.amazon.smithy:smithy-aws-iam-traits:1.14.1"
    ),
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    )
  )
