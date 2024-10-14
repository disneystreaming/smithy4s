ThisBuild / scalaVersion := "2.13.10"

lazy val a = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  )
lazy val b = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  )

lazy val usage = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(a, b)

val root = project
  .in(file("."))
  .aggregate(a, b, usage)
