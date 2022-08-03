lazy val commonSettings = Def.settings(
  scalaVersion := "2.13.6",
  smithy4sInputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_input",
  Compile / smithy4sAllowedNamespaces := List(
    "aws.iam",
    "smithy4s.example"
  ),
  libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
  libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % "1.21.0-rc1" % Smithy4s
)

lazy val p1 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    smithy4sOutputDir in Compile := baseDirectory.value / "smithy_output",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    )
  )
lazy val p2 = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(commonSettings)
  .settings(
    smithy4sOutputDir in Compile := baseDirectory.value / "smithy_output",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.iam",
      "smithy4s.example"
    ),
    Compile / smithy4sExcludedNamespaces := List("smithy4s.toexclude")
  )
