lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "software.amazon.smithy" % "smithy-rules-engine" % smithy4s.codegen.BuildInfo.smithyVersion % Smithy4s
    ),
    Compile / smithy4sAllowedNamespaces := List("smithy.rules")
  )
