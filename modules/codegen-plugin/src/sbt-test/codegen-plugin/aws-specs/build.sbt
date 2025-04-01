lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.15",
    smithy4sAwsSpecs ++= Seq(AWS.dynamodb)
  )
