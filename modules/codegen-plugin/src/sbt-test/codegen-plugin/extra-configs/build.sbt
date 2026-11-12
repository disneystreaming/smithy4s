@transient lazy val checkIsolation = taskKey[Unit]("Check Compile/Test output isolation")

lazy val root = (project in file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
    Smithy4sCodegenPlugin.defaultSettings(Test),
    checkIsolation := {
      val main = (Compile / smithy4sCodegen).value.toSet
      val test = (Test / smithy4sCodegen).value.toSet
      val mainNames = main.map(_.getName)
      val testNames = test.map(_.getName)
      assert(mainNames.contains("ExampleStruct.scala"), s"Missing main output: $main")
      assert(!mainNames.contains("AddedTest.scala"), s"Test output leaked into main: $main")
      assert(testNames.contains("TestStruct.scala") && testNames.contains("AddedTest.scala"), s"Missing test outputs: $test")
      assert(main.intersect(test).isEmpty, s"Shared output paths: ${main.intersect(test)}")
      val mainRoots = Seq((Compile / smithy4sOutputDir).value, (Compile / smithy4sResourceDir).value)
      val testRoots = Seq((Test / smithy4sOutputDir).value, (Test / smithy4sResourceDir).value)
      assert(main.forall(f => mainRoots.exists(root => f.toPath.startsWith(root.toPath))), s"Internal main paths leaked: $main")
      assert(test.forall(f => testRoots.exists(root => f.toPath.startsWith(root.toPath))), s"Internal test paths leaked: $test")
    }
  )
