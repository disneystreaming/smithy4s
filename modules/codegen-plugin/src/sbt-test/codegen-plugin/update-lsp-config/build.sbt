import sbt.io.IO
val subproj = project

val subproj2 = project.enablePlugins(Smithy4sCodegenPlugin)

val root = project
  .in(file("."))
  .aggregate(subproj, subproj2)
  .settings(
    TaskKey[Unit]("checkSmithyBuild") := {
      val generated = IO.readLines(file(".") / "smithy-build.json")
      val expected = IO.readLines(file(".") / "expected.json")
      val compare = s"""|generated:
                        |${generated.mkString("\n")}
                        |===================================
                        |expected:
                        |${expected.mkString("\n")}
                        |===================================
                        |""".stripMargin

      assert(generated == expected, s"content are not the same:\n $compare")
      ()
    }
  )
