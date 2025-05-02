ThisBuild / resolvers += "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots"

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.alloy" %% "alloy-openapi" % "0.3.17-1-e0ac29-SNAPSHOT"
    ),
    Compile / smithyBuild := Some(baseDirectory.value / "smithy-build.json"),
    TaskKey[Unit]("checkOpenApi") := {
      val resourceDir = (Compile / smithy4sResourceDir).value
      val content =
        IO.readLines(
          resourceDir / "smithy4s.example.ObjectService.json"
        ).filter(_.trim().nonEmpty)
          .mkString("")
          .trim()
      if (!content.contains("X-Bar") || !content.contains("3.1.0"))
        sys.error("OpenAPI transformation was not applied")
    }
  )
