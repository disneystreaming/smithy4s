lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    ),
    Compile / scalacOptions ++= Seq(
      "-Xsource:3", "-P:kind-projector:underscore-placeholders"
    ),
    Compile / smithyBuild := Some(baseDirectory.value / "smithy-build.json"),
    TaskKey[Unit]("checkOpenApi") := {
      val resourceDir = (Compile / smithy4sResourceDir).value
      val content =
        IO.readLines(
          resourceDir / "smithy4s.example.ObjectService.json"
        ).filter(_.trim().nonEmpty)
          .mkString("")
          .trim()
      if (!content.contains("X-Bar") || !content.contains("3.1.0"))
        sys.error("OpenAPI transformation was not applied")
    }
  )
