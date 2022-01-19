---
sidebar_label: Installation (SBT)
title: Installation (SBT)
---

`smithy4s-sbt-codegen` is a code generating SBT plugin that creates `.scala` models and stubs out of the `.smithy` specs. The generated code does not depend on any third-party dependency for compilation, whether http-related, json-related or otherwise. It does however depend on a "core" library that contains a number of interfaces implemented by the generated code.

In `project/plugins.sbt` :

```scala
addSbtPlugin("com.disneystreaming.smithy4s"  % "smithy4s-sbt-codegen" % "@VERSION@")
```

and enable the plugin in the desired sbt module :

```scala
import smithy4s.codegen.Smithy4sCodegenPlugin

val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  // version for smithy4s-core is sourced from Smithy4sCodegenPlugin
  .settings(libraryDependencies += "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value)
```

This will enable the plugin on `myModule`. We also need to add `smithy4s-core ` here since it is needed for compiling the generated code.

By default, the plugin will look in the `$MY_MODULE/src/main/smithy` directory and will write scala code in `$MY_MODULE/target/scala-<version>/src_managed/` when invoking `compile`. The paths are configurable via the `smithy4sInputDir` and `smithy4sOutputDir` settings keys.

For example, in order for the plugin to source `.smithy` specs from `./smithy_input` (inside the folder where our `build.sbt` is) and output the generated files into `./smithy_output`.

```scala
val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "@SCALA_VERSION@",
    smithy4sInputDir in Compile  := (baseDirectory in ThisBuild).value / "smithy_input",
    smithy4sOutputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_output",
    libraryDependencies += "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
  )
```


