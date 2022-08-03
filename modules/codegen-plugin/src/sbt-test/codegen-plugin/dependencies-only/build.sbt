lazy val p1 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.6",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam"
    ),
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % "1.21.0-rc1" % Smithy4s,
    smithy4sOutputDir in Compile := baseDirectory.value / "smithy_output"
  )
