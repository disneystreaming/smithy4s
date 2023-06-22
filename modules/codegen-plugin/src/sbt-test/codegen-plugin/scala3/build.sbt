lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "3.2.1",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )
