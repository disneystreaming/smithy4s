lazy val commonSettings = Def.settings(
  scalaVersion := "2.13.6",
  Compile / smithy4sInputDir := (ThisBuild / baseDirectory).value / "smithy_input",
  Compile / smithy4sAllowedNamespaces := List(
    "aws.iam",
    "smithy4s.example"
  ),
  libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
  libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % smithy4s.codegen.BuildInfo.smithyVersion % Smithy4s
)

lazy val p1 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    Compile / smithy4sOutputDir := baseDirectory.value / "smithy_output",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    )
  )
lazy val p2 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    Compile / smithy4sOutputDir := baseDirectory.value / "smithy_output",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    ),
    Compile / smithy4sExcludedNamespaces := List("smithy4s.toexclude")
  )
