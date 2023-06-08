ThisBuild / scalaVersion := "2.13.11"

lazy val foo = (project in file("foo"))

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(Compile / smithy4sInternalDependenciesAsJars := Nil)
  .dependsOn(foo)
