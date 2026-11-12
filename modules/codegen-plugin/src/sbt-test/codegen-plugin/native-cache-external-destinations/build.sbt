@transient lazy val checkOutputs = taskKey[Unit]("Check codegen outputs and invocation count")
@transient lazy val deleteGeneratedOnSbt2 = taskKey[Unit]("Delete generated outputs on sbt2")
@transient lazy val rememberExternal = taskKey[Unit]("Remember external output modification times")
@transient lazy val checkExternalUnchanged = taskKey[Unit]("Check that external outputs were not rewritten")
@transient lazy val damageExternalOnSbt2 = taskKey[Unit]("Damage external text and copied-resource outputs")
@transient lazy val corruptExternalSameSizeOnSbt2 = taskKey[Unit]("Overwrite external outputs with same-size different content on sbt2")
lazy val expectedRuns = settingKey[Int]("Expected transformer invocations")

lazy val transformation = project.settings(
  scalaVersion := "2.13.18",
  libraryDependencies += "software.amazon.smithy" % "smithy-build" % smithy4s.codegen.BuildInfo.smithyVersion
)

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    expectedRuns := 1,
    Compile / smithy4sOutputDir := baseDirectory.value / "outside-src",
    Compile / smithy4sResourceDir := baseDirectory.value / "outside-resources",
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
      val count = IO.readLines(baseDirectory.value / "generation-count").size
      assert(count == expectedRuns.value, s"Expected ${expectedRuns.value} generations, got $count")
      val sourceDir = (Compile / smithy4sOutputDir).value.toPath
      val resourceDir = (Compile / smithy4sResourceDir).value.toPath.resolve("META-INF/smithy")
      val expectedFiles = Set(
        sourceDir.resolve("cache/test/Alpha.scala"),
        sourceDir.resolve("cache/test/Other.scala"),
        resourceDir.resolve("model.smithy"),
        resourceDir.resolve("generated-metadata.smithy"),
        resourceDir.resolve("smithy4s.tracking.smithy"),
        resourceDir.resolve("manifest")
      ).map(_.toAbsolutePath.normalize())
      val actualFiles = files.map(_.toPath.toAbsolutePath.normalize()).toSet
      assert(actualFiles == expectedFiles, s"Expected $expectedFiles, got $actualFiles")
      assert(files.forall(_.isFile), s"Missing outputs: $files")
      assert(IO.read(files.find(_.getName == "Alpha.scala").get).contains("first"))
      assert(IO.read(files.find(_.getName == "model.smithy").get).contains("first: String"))
    },
    deleteGeneratedOnSbt2 := {
      val files = (Compile / smithy4sCodegen).value
      val metadata = (Compile / smithy4sGeneratedSmithyMetadataFile).value
      if (sbtVersion.value.startsWith("2.")) IO.delete(files :+ metadata)
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
    }
  )
