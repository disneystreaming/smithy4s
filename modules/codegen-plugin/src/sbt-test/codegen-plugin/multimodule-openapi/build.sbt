ThisBuild / scalaVersion := "2.13.18"

lazy val upstream = (project in file("upstream"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    TaskKey[Unit]("checkOpenApiSpecs") := {
      val resources = (Compile / resourceManaged).value
      assert((resources / "upstream.UpstreamService.json").exists, "upstream.UpstreamService.json should be in upstream")
    }
  )

lazy val downstream = (project in file("downstream"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(upstream)
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    TaskKey[Unit]("checkOpenApiSpecs") := {
      val resources = (Compile / resourceManaged).value
      val sources = (Compile / sourceManaged).value / "smithy4s"
      assert((resources / "downstream.DownstreamService.json").exists, "downstream.DownstreamService.json should be in downstream")
      // the upstream service's spec is already in the upstream jar, like its Scala code
      assert(!(resources / "upstream.UpstreamService.json").exists, "upstream.UpstreamService.json should not be in downstream")
      assert(!(sources / "upstream" / "UpstreamService.scala").exists, "upstream/UpstreamService.scala should not be in downstream")
    }
  )
