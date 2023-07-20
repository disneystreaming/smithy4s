lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    Compile / smithy4sAllowedNamespaces := List(
      "aws.api",
      "aws.auth",
      "aws.customizations",
      "aws.protocols",
      "com.amazonaws.dynamodb",
    ),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy"   % "aws-dynamodb-spec" % "2023.02.10"
    ),
    Compile / smithy4sOutputDir := baseDirectory.value / "smithy_output"
  )
