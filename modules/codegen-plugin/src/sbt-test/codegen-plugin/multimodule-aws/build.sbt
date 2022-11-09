import smithy4s.codegen.BuildInfo._

ThisBuild / scalaVersion := "2.13.8"

lazy val foo = (project in file("foo"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.smithy" % "smithy-aws-traits" % smithyVersion % Smithy4s,
      "com.disneystreaming.smithy4s" %% "smithy4s-aws-kernel" % smithy4sVersion.value
    )
  )

lazy val bar = (project in file("bar"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .dependsOn(foo)
