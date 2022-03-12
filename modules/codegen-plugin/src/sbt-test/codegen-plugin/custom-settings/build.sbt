lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.6",
    smithy4sInputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_input",
    smithy4sOutputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_output",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    ),
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % "1.14.1" % "smithy4s"
  )
