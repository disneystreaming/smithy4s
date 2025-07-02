lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "io.github.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "io.github.disneystreaming.smithy4s" %% "smithy4s-dynamic" % smithy4sVersion.value,
      "io.github.disneystreaming.alloy" % "alloy-core" % "0.3.23"
    )
  )
