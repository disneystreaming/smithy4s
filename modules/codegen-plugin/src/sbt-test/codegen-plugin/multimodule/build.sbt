ThisBuild / scalaVersion := "2.13.10"

lazy val root = project
  .in(file("."))
  .settings(
    TaskKey[Unit]("checkSmithyBuild") := {
      val expectedLines = Set(
        "version",
        "src/main/smithy",
        "software.amazon.smithy:smithy-waiters:1.38.0",
        "com.disneystreaming.alloy:alloy-core:0.2.7",
        "com.disneystreaming.smithy4s:smithy4s-protocol:",
        "custom",
        "attribute"
      )
      val content =
        IO.readLines(baseDirectory.value / "smithy-build.json")
          .filter(_.trim().nonEmpty)
          .mkString("\n")
          .trim()

      expectedLines.foreach { expected =>
        require(
          content.contains(expected),
          s"""|Could not find `$expected in the generate file:
              |
              |$content
              |""".stripMargin
        )
      }
      ()
    }
  )
  .aggregate(foo, inBetween, bar)

lazy val foo = (project in file("foo"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" % "smithy4s-protocol" % smithy4sVersion.value % Smithy4s
    )
  )

lazy val inBetween = (project in file("inBetween"))
  .dependsOn(foo)

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(inBetween)
