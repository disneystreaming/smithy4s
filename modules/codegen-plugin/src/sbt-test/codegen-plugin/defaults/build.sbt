lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-dynamic" % smithy4sVersion.value
    ),
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
      val expected = """|{
                        |    "version": "1.0",
                        |    "imports": [
                        |        "src/main/smithy"
                        |    ],
                        |    "maven": {
                        |        "dependencies": [
                        |            "com.disneystreaming.alloy:alloy-core:0.2.6"
                        |        ],
                        |        "repositories": [
                        |        ]
                        |    }
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
