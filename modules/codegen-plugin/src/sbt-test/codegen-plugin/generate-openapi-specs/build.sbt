@transient lazy val checkOpenApiInventory = taskKey[Unit]("Check the generated OpenAPI inventory")

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    ),
    Compile / smithy4sGenerateOpenApiSpecs := false,
    checkOpenApiInventory := {
      val files = (Compile / smithy4sCodegen).value
      val expected = (Compile / smithy4sGenerateOpenApiSpecs).value
      assert(files.exists(_.getName == "HelloService.scala"), s"Missing Scala output: $files")
      assert(
        files.exists(_.getName == "foo.HelloService.json") == expected,
        s"Unexpected OpenAPI inventory (enabled = $expected): $files"
      )
    }
  )
