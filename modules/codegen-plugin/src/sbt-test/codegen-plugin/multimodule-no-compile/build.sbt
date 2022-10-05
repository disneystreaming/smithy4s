ThisBuild / scalaVersion := "2.13.8"

lazy val foo = (project in file("foo"))

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(Compile / smithy4sAggregateLocalDependencies := false)
  .dependsOn(foo)
