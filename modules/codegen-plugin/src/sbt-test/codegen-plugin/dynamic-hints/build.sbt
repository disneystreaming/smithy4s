import smithy4s.codegen.Smithy4sCodegenPlugin.autoImport.smithy4sOutputDir
lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    Compile / smithy4sRenderDynamicHintBindings := true,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    ),
    TaskKey[Unit]("check") := {
      val actual = IO
        .readLines(
          (Compile / smithy4sOutputDir).value / "smithy4s" / "example" / "Test.scala"
        )
        .mkString("\n")
      val hasDynamicHints = actual.contains("Hints.dynamic(")
      if (!hasDynamicHints) {
        sys.error("Found no dynamic hints in output")
      }
    }
  )
