lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-dynamic" % smithy4sVersion.value
    ),
    TaskKey[Unit]("checkSmithyBuild") := {
      val expectedLines = Set(
        "version",
        "src/main/smithy",
        "com.disneystreaming.alloy:alloy-core:0.2.8"
      )
      val content =
        IO.readLines(baseDirectory.value / "smithy-build.json")
          .filter(_.trim().nonEmpty)
          .mkString("\n")
          .trim()

      expectedLines.foreach { expected =>
        require(
          content.contains(expected),
          s"Could not find `$expected in the generate file."
        )
      }
      ()
    }
  )
