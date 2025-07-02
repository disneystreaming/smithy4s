lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "io.github.disneystreaming.smithy4s" %% "smithy4s-aws-kernel" % smithy4sVersion.value
    )
  )
