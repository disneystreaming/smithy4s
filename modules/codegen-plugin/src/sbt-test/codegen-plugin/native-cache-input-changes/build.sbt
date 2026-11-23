@transient lazy val checkOutputs = taskKey[Unit]("Check codegen outputs and invocation count")
lazy val expectedRuns = settingKey[Int]("Expected transformer invocations")
lazy val expectedModel = settingKey[String]("Expected model version")

lazy val transformation = project.settings(
  scalaVersion := "2.13.18",
  libraryDependencies += "software.amazon.smithy" % "smithy-build" % smithy4s.codegen.BuildInfo.smithyVersion
)

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    expectedRuns := 1,
    expectedModel := "first",
    Compile / smithy4sModelTransformers := {
      System.setProperty("smithy4s.test.counter", (baseDirectory.value / "generation-count").getAbsolutePath)
      List("count-generation")
    },
    Compile / smithy4sAllDependenciesAsJars += {
      val ref: Any = (transformation / Compile / packageBin).value
      ref match {
        case f: java.io.File => f
        case other: xsbti.VirtualFileRef => fileConverter.value.toPath(other).toFile
      }
    },
    checkOutputs := {
      val files = (Compile / smithy4sCodegen).value
      val expected = expectedModel.value
      val count = IO.readLines(baseDirectory.value / "generation-count").size
      assert(count == expectedRuns.value, s"Expected ${expectedRuns.value} generations, got $count")
      val sourceDir = (Compile / smithy4sOutputDir).value.toPath
      val resourceDir = (Compile / smithy4sResourceDir).value.toPath.resolve("META-INF/smithy")
      val expectedFiles = Set(
        sourceDir.resolve("cache/test/Alpha.scala"),
        sourceDir.resolve(s"cache/test/${if (expected == "first") "Other" else "Beta"}.scala"),
        resourceDir.resolve("model.smithy"),
        resourceDir.resolve("generated-metadata.smithy"),
        resourceDir.resolve("smithy4s.tracking.smithy"),
        resourceDir.resolve("manifest")
      ).map(_.toAbsolutePath.normalize())
      val actualFiles = files.map(_.toPath.toAbsolutePath.normalize()).toSet
      assert(actualFiles == expectedFiles, s"Expected $expectedFiles, got $actualFiles")
      assert(files.forall(_.isFile), s"Missing outputs: $files")
      val alpha = files.find(_.getName == "Alpha.scala").get
      assert(IO.read(alpha).contains(expected), s"Wrong Alpha content: ${IO.read(alpha)}")
      val packaged = files.find(_.getName == "model.smithy").get
      assert(IO.read(packaged).contains(s"$expected: String"), s"Wrong copied model: ${IO.read(packaged)}")
    }
  )
