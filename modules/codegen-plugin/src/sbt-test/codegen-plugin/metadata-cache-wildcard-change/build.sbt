@transient lazy val checkMetadata = taskKey[Unit]("Check generated metadata")
@transient lazy val installNativeChecks = taskKey[Unit]("Load native event assertions on sbt2 only")
@transient lazy val checkMetadataHit = taskKey[Unit]("Verify native metadata cache hits")

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
      assert(
        IO.read(files.head).contains(s"""metadata smithy4sWildcardArgument = "$expected""""),
        s"Unexpected metadata content: ${IO.read(files.head)}"
      )
    }
  )
