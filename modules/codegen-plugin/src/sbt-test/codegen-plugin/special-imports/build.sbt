lazy val root = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.10",
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,

    // 0.0.1-SNAPSHOT works fine because the file path doesn't have urlencoded characters
    libraryDependencies += "org.polyvariant" %% "test-library-core" % "0.0.1+123-SNAPSHOT" % Smithy4s,
    resolvers ++= Resolver.sonatypeOssRepos("snapshots")
  )
