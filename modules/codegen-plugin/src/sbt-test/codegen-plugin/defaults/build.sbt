lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "io.github.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "io.github.disneystreaming.smithy4s" %% "smithy4s-dynamic" % smithy4sVersion.value
    ),
    TaskKey[Unit]("checkSmithyBuild") := {
      val expectedLines = Set(
        "version",
        "src/main/smithy",
        s"io.github.disneystreaming.alloy:alloy-core:${smithy4s.codegen.BuildInfo.alloyVersion}"
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
