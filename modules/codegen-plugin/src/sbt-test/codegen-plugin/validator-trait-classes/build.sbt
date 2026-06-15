lazy val externalLibrary = project
  .settings(
    autoScalaLibrary := false,
    crossPaths := false,
    libraryDependencies ++= Seq(
      "software.amazon.smithy" % "smithy-model" % "1.57.1"
    )
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "3.3.6",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    ),
    Compile / smithy4sAllDependenciesAsJars += {
      val ref: Any = (externalLibrary / Compile / packageBin).value
      ref match {
        case f: java.io.File => f
        case other: xsbti.VirtualFileRef =>
          fileConverter.value.toPath(other).toFile
      }
    }
  )
