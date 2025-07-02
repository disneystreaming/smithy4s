lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "3.3.0",
    libraryDependencies ++= Seq(
      "io.github.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    )
  )
