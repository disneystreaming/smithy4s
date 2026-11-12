@transient lazy val checkMetadata = taskKey[Unit]("Check generated metadata")
@transient lazy val deleteMetadataOnSbt2 = taskKey[Unit]("Delete metadata on sbt2")
@transient lazy val installNativeChecks = taskKey[Unit]("Load native event assertions on sbt2 only")
@transient lazy val checkMetadataHit = taskKey[Unit]("Verify native metadata cache hits")
@transient lazy val rememberMetadata = taskKey[Unit]("Remember metadata modification time")
@transient lazy val checkMetadataUnchanged = taskKey[Unit]("Check metadata was not rewritten")
@transient lazy val damageMetadataOnSbt2 = taskKey[Unit]("Damage external metadata")

ThisBuild / checkMetadataHit := {
  assert(!sbtVersion.value.startsWith("2."), "The sbt2 native cache assertions were not loaded")
}

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    Compile / smithy4sWildcardArgument := "_",
    installNativeChecks := {
      if (sbtVersion.value.startsWith("2.")) {
        val base = baseDirectory.value
        IO.copyFile(base / "native-check.sbt2", base / "zz-native-check.sbt")
      }
    },
    checkMetadata := {
      val files = (Compile / smithy4sGeneratedSmithyFiles).value
      val expected = (Compile / smithy4sWildcardArgument).value
      assert(files.size == 1, s"Expected one metadata file, got $files")
      assert(files.head.isFile, s"Missing metadata file: ${files.head}")
      if (sbtVersion.value.startsWith("2.")) {
        assert(files.head == (Compile / smithy4sGeneratedSmithyMetadataFile).value, s"Internal path leaked: $files")
      }
      assert(
        IO.read(files.head).contains(s"""metadata smithy4sWildcardArgument = "$expected""""),
        s"Unexpected metadata content: ${IO.read(files.head)}"
      )
    },
    deleteMetadataOnSbt2 := {
      if (sbtVersion.value.startsWith("2.")) {
        IO.delete((Compile / smithy4sGeneratedSmithyMetadataFile).value)
      }
    },
    rememberMetadata := {
      val file = (Compile / smithy4sGeneratedSmithyFiles).value.head
      IO.write(baseDirectory.value / "metadata-time", java.nio.file.Files.getLastModifiedTime(file.toPath).toString)
    },
    checkMetadataUnchanged := {
      val file = (Compile / smithy4sGeneratedSmithyFiles).value.head
      assert(IO.read(baseDirectory.value / "metadata-time") == java.nio.file.Files.getLastModifiedTime(file.toPath).toString)
    },
    damageMetadataOnSbt2 := {
      if (sbtVersion.value.startsWith("2.")) {
        val file = (Compile / smithy4sGeneratedSmithyMetadataFile).value
        IO.delete(file)
        IO.write(file, "damaged metadata")
      }
    }
  )
