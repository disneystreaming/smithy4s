lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-dynamic" % smithy4sVersion.value
    )
  )
