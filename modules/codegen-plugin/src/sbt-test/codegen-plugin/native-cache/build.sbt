@transient lazy val checkOutputs = taskKey[Unit]("Check codegen outputs and invocation count")
@transient lazy val addUnrelated = taskKey[Unit]("Add files not owned by codegen")
@transient lazy val deleteGeneratedOnSbt2 = taskKey[Unit]("Delete generated outputs on sbt2")
@transient lazy val checkEmpty = taskKey[Unit]("Check an empty output inventory")
@transient lazy val checkDependencyPath = taskKey[Unit]("Check that the dependency jar path is stable")
@transient lazy val rememberExternal = taskKey[Unit]("Remember external output modification times")
@transient lazy val checkExternalUnchanged = taskKey[Unit]("Check that external outputs were not rewritten")
@transient lazy val damageExternalOnSbt2 = taskKey[Unit]("Damage external text and copied-resource outputs")
@transient lazy val corruptExternalSameSizeOnSbt2 = taskKey[Unit]("Overwrite external outputs with same-size different content on sbt2")
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
    addUnrelated := {
      IO.write((Compile / smithy4sOutputDir).value / "orphan.scala", "unrelated source")
      IO.write((Compile / smithy4sResourceDir).value / "unrelated.txt", "unrelated resource")
    },
    checkOutputs := {
      val files = (Compile / smithy4sCodegen).value
      val expected = expectedModel.value
      val count = IO.readLines(baseDirectory.value / "generation-count").size
      assert(count == expectedRuns.value, s"Expected ${expectedRuns.value} generations, got $count")
      assert(files.nonEmpty && files.forall(_.isFile), s"Missing outputs: $files")
      val names = files.map(_.getName).toSet
      assert(!names.contains("orphan.scala") && !names.contains("unrelated.txt"), s"Unrelated files returned: $names")
      assert(names.contains("Alpha.scala"), s"Missing Alpha.scala: $names")
      assert(names.contains("generated-metadata.smithy"), s"Missing resource metadata: $names")
      assert(names.contains("model.smithy"), s"Missing packaged input: $names")
      assert(names.contains(if (expected == "first") "Other.scala" else "Beta.scala"), s"Missing expected model: $names")
      assert(!names.contains(if (expected == "first") "Beta.scala" else "Other.scala"), s"Stale output returned: $names")
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
      val alpha = files.find(_.getName == "Alpha.scala").get
      assert(IO.read(alpha).contains(expected), s"Wrong Alpha content: ${IO.read(alpha)}")
      assert(((Compile / smithy4sResourceDir).value / "unrelated.txt").isFile)
      val packaged = files.find(_.getName == "model.smithy").get
      assert(IO.read(packaged).contains(s"$expected: String"), s"Wrong copied model: ${IO.read(packaged)}")
    },
    deleteGeneratedOnSbt2 := {
      val files = (Compile / smithy4sCodegen).value
      val metadata = (Compile / smithy4sGeneratedSmithyMetadataFile).value
      if (sbtVersion.value.startsWith("2.")) IO.delete(files :+ metadata)
    },
    checkEmpty := {
      val files = (Compile / smithy4sCodegen).value
      val count = IO.readLines(baseDirectory.value / "generation-count").size
      assert(files.isEmpty, s"Expected no generated files, got $files")
      assert(count == expectedRuns.value, s"Expected ${expectedRuns.value} generations, got $count")
    },
    rememberExternal := {
      val files = (Compile / smithy4sCodegen).value
      IO.writeLines(baseDirectory.value / "external-times", files.map(f => s"${f.getAbsolutePath}:${java.nio.file.Files.getLastModifiedTime(f.toPath)}"))
    },
    checkExternalUnchanged := {
      val files = (Compile / smithy4sCodegen).value
      val times = files.map(f => s"${f.getAbsolutePath}:${java.nio.file.Files.getLastModifiedTime(f.toPath)}")
      assert(IO.readLines(baseDirectory.value / "external-times") == times, "Identical external outputs were rewritten")
    },
    damageExternalOnSbt2 := {
      if (sbtVersion.value.startsWith("2.")) {
        val alpha = (Compile / smithy4sOutputDir).value / "cache/test/Alpha.scala"
        val model = (Compile / smithy4sResourceDir).value / "META-INF/smithy/model.smithy"
        // Do not mutate a CAS target on runtimes that materialize with links.
        IO.delete(alpha)
        IO.write(alpha, "damaged source")
        IO.delete(model)
      }
    },
    corruptExternalSameSizeOnSbt2 := {
      if (sbtVersion.value.startsWith("2.")) {
        val files = Seq(
          (Compile / smithy4sOutputDir).value / "cache/test/Alpha.scala",
          (Compile / smithy4sResourceDir).value / "META-INF/smithy/model.smithy"
        )
        files.foreach { f =>
          val size = IO.readBytes(f).length
          assert(size > 0, s"Unexpected empty output: $f")
          // Same byte length, different content: only a content comparison detects this.
          IO.delete(f)
          IO.write(f, Array.fill(size)('x'.toByte))
        }
      }
    },
    checkDependencyPath := {
      val ref: Any = (transformation / Compile / packageBin).value
      val jar = ref match {
        case f: java.io.File => f
        case other: xsbti.VirtualFileRef => fileConverter.value.toPath(other).toFile
      }
      val saved = baseDirectory.value / "dependency-jar-path"
      if (saved.exists()) assert(IO.read(saved) == jar.getAbsolutePath)
      else IO.write(saved, jar.getAbsolutePath)
    }
  )
