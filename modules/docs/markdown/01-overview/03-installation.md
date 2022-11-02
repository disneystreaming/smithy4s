---
sidebar_label: Installation
title: Installation
---

Smithy4s generates Scala code from a Smithy model. The generated code includes traits for any services you might define, as well as case classes for models used in these services. It has no dependencies on external libraries or any specific protocol like HTTP or JSON. It does, however, depend on a "core" library that contains a number of interfaces implemented by the generated code.

## SBT

_For mill support, see [Mill](#mill) below._

`smithy4s-sbt-codegen` is a code generating sbt plugin that creates `.scala` files corresponding to the provided `.smithy` specs.

In `project/plugins.sbt`:

```scala
addSbtPlugin("com.disneystreaming.smithy4s"  % "smithy4s-sbt-codegen" % "@VERSION@")
```

and enable the plugin in the desired sbt module:

```scala
import smithy4s.codegen.Smithy4sCodegenPlugin

val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  // version for smithy4s-core is sourced from Smithy4sCodegenPlugin
  .settings(libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value)
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
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  )
```

## Mill

`smithy4s-mill-codegen-plugin` is a plugin to enable Smithy4s code generation on a `mill` module.

For example, here, we enabled it on the `example` module:

```scala
import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.8"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )
}
```

By default, the `mill` plugin will look for Smithy files under the `$MY_MODULE/smithy` directory. The generated code ends up in `out/$MY_MODULE/smithy4sOutputDir.dest/scala/`, again, by default. Code generation happens automatically when you before you `compile` the module. The paths are configurable via the `smithy4sInputDir` and `smithy4sOutputDir` tasks.

For example, here we'll read Smithy files from `smithy_input` and write to `smithy_output`.

```scala
import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.8"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )

  override def smithy4sInputDir = T.source {
    PathRef(T.ctx().workspace / "smithy_input")
  }
  override def smithy4sOutputDir = T {
    PathRef(T.ctx().workspace / "smithy_output")
  }
}
```