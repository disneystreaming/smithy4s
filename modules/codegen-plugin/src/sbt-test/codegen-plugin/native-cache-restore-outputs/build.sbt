@transient lazy val checkOutputs = taskKey[Unit]("Check codegen outputs and that generation ran once")
@transient lazy val deleteGeneratedOnSbt2 = taskKey[Unit]("Delete generated outputs on sbt2")

lazy val transformation = project.settings(
  scalaVersion := "2.13.18",
  libraryDependencies += "software.amazon.smithy" % "smithy-build" % smithy4s.codegen.BuildInfo.smithyVersion
)

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
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
      assert(count == 1, s"Expected 1 generation, got $count")
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
    },
    deleteGeneratedOnSbt2 := {
      val files = (Compile / smithy4sCodegen).value
      val metadata = (Compile / smithy4sGeneratedSmithyMetadataFile).value
      if (sbtVersion.value.startsWith("2.")) IO.delete(files :+ metadata)
    }
  )
