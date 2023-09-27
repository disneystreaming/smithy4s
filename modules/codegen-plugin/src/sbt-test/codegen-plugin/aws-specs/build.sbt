lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    smithy4sAwsSpecs ++= Seq(AWS.dynamodb)
  )
