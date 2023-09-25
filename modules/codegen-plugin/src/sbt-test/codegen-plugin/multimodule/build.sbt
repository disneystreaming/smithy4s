ThisBuild / scalaVersion := "2.13.10"

lazy val root = project
  .in(file("."))
  .settings(
    /**
      * this test is sensitive to whitespaces
      * that's why we trim the whole thing and
      * we filter out lines that are empty after whitespace trimming
      */
    TaskKey[Unit]("checkSmithyBuild") := {
      val content =
        IO.readLines(baseDirectory.value / "smithy-build.json")
          .filter(_.trim().nonEmpty)
          .mkString("\n")
          .trim()
      val expected =
        """|{
           |    "version": "1.0",
           |    "imports": [
           |        "src/main/smithy"
           |    ],
           |    "maven": {
           |        "dependencies": [
           |            "software.amazon.smithy:smithy-waiters:1.38.0",
           |            "com.disneystreaming.alloy:alloy-core:0.2.7",
           |            "com.disneystreaming.smithy4s:smithy4s-protocol:dev-SNAPSHOT"
           |        ],
           |        "repositories": [
           |        ]
           |    },
           |    "custom": "attribute"
           |}""".stripMargin.trim()

      require(
        content == expected,
        s"""|
            |Actual:
            |$content
            |--------
            |Expected:
            |$expected""".stripMargin
      )
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
