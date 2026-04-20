lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    smithy4sAwsSpecEntries ++= Seq(AWS.dynamodb)
  )
