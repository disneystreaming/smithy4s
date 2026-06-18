lazy val transformation = project
  .settings(
    scalaVersion := "2.12.20",
    libraryDependencies ++= Seq(
      "software.amazon.smithy" % "smithy-build" % "1.57.1",
      "ch.epfl.scala" % "spec-traits" % "2.2.0-M2"
    )
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "3.3.6",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" % "spec-traits" % "2.2.0-M2" % Smithy4s,
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
    ),
    Compile / smithy4sModelTransformers := List(
      "my-transformation"
    ),
    Compile / smithy4sAllowedNamespaces := List("my.input"),
    Compile / smithy4sAllDependenciesAsJars += {
      val ref: Any = (transformation / Compile / packageBin).value
      ref match {
        case f: java.io.File => f
        case other: xsbti.VirtualFileRef =>
          fileConverter.value.toPath(other).toFile
      }
    }
  )
