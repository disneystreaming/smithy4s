import sbt.io.IO
val subproj = project

val subproj2 = project.enablePlugins(Smithy4sCodegenPlugin)
//            "com.disneystreaming.smithy4s:smithy4s-protocol:${smithy4sVersion.value}"
val root = project
  .in(file("."))
  .aggregate(subproj, subproj2)
  .settings(
    TaskKey[Unit]("checkSmithyBuild") := {
      val generated = IO.readLines(file(".") / "smithy-build.json")
      val expected = s"""{
                        |    "version" : "1.0",
                        |    "sources" : [
                        |        "subproj2/src/main/smithy",
                        |        "subproj2/target/scala-2.12/src_managed/main/smithy"
                        |    ],
                        |    "maven" : {
                        |        "dependencies" : [
                        |            "com.disneystreaming.alloy:alloy-core:0.3.13",
                        |            "com.disneystreaming.smithy4s:smithy4s-protocol:${smithy4sVersion.value}"
                        |        ],
                        |        "repositories" : [
                        |            {
                        |                "url" : "https://oss.sonatype.org/content/repositories/snapshots"
                        |            },
                        |            {
                        |                "url" : "https://s01.oss.sonatype.org/content/repositories/snapshots"
                        |            }
                        |        ]
                        |    }
                        |}""".stripMargin
      val compare = s"""|generated:
                        |${generated.mkString("\n")}
                        |===================================
                        |expected:
                        |${expected}
                        |===================================
                        |""".stripMargin

      assert(generated.mkString("\n") == expected, s"content are not the same:\n $compare")
      ()
    }
  )
