import sbt.io.IO
val subproj = project

val subproj2 = project.enablePlugins(Smithy4sCodegenPlugin)

val root = project
  .in(file("."))
  .aggregate(subproj, subproj2)
  .settings(
    TaskKey[Unit]("checkSmithyBuild") := {
      val generated = IO.readLines(file(".") / "smithy-build.json").mkString("\n")
      val expected = IO.readLines(file(".") / "expected.json").mkString("\n").replace("${SMITHY4S_VERSION}", smithy4sVersion.value)
      val compare = s"""|generated:
                        |$generated
                        |===================================
                        |expected:
                        |$expected
                        |===================================
                        |""".stripMargin

      assert(generated == expected, s"content are not the same:\n $compare")
      ()
    }
  )
