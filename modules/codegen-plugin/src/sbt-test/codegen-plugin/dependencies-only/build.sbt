lazy val p1 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.6",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam"
    ),
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % smithy4s.codegen.BuildInfo.smithyVersion % Smithy4s,
    Compile / smithy4sOutputDir := baseDirectory.value / "smithy_output"
  )
