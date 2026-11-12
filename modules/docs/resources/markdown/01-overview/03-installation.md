---
sidebar_label: Installation
title: Installation
---

Smithy4s generates Scala code from a Smithy model. The generated code includes traits for any services you might define, as well as case classes for models used in these services. It has no dependencies on external libraries or any specific protocol like HTTP or JSON. It does, however, depend on a "core" library that contains a number of interfaces implemented by the generated code.

## SBT

_For mill support, see [Mill](#mill) below._

`smithy4s-sbt-codegen` is a code generating sbt plugin that creates `.scala` files corresponding to the provided `.smithy` specs.
When using sbt 2, version 2.0.4 or later is required.

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

By default, the plugin will look in the `$MY_MODULE/src/main/smithy` directory and will write scala code in `$MY_MODULE/target/scala-<version>/src_managed/` when invoking `compile`. The paths are configurable via the `smithy4sInputDirs` and `smithy4sOutputDir` settings keys.

For example, in order for the plugin to source `.smithy` specs from `./smithy_input` (inside the folder where our `build.sbt` is) and output the generated files into `./smithy_output`.

```scala
val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "@SCALA_VERSION@",
    Compile / smithy4sInputDirs := Seq((ThisBuild / baseDirectory).value / "smithy_input"),
    Compile / smithy4sOutputDir := (ThisBuild / baseDirectory).value / "smithy_output",
    libraryDependencies += "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value
  )
```

### sbt 2 caching and custom tasks

On sbt 2, generation uses the native task cache. Custom destinations outside
`rootOutputDirectory`, including `smithy_output` in the example above, remain
supported: the plugin caches an exact inventory in an internal staging directory
and materializes those files at the configured destinations. Unchanged files are
not rewritten; missing or modified files are restored without rerunning generation.
The public tasks still return the configured files, never internal staging files.
Obsolete generated files are not removed automatically.

If you replace a File-returning task definition on sbt 2, wrap your replacement in
`Def.uncached`, for example:

```scala
Compile / smithyBuild := Def.uncached {
  Some(baseDirectory.value / "smithy-build.json")
}
```

This also applies to custom overrides of `smithy4sCodegen` and
`smithy4sGeneratedSmithyFiles`, whose result type remains `Seq[File]`. The plugin's
own uncached facade does not wrap replacement definitions. For cross-built sbt
1/2 builds, provide a build-local no-op `Def.uncached` compatibility extension on
sbt 1; the plugin's internal extension is not exported to build definitions.

## Mill

`smithy4s-mill-codegen-plugin` is a plugin to enable Smithy4s code generation on a `mill` module.

### Mill 0.11.x / 0.12.x

For example, here, we enabled it on the `example` module:

```scala
import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.18"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )
}
```

:::warning

For mill 0.12.x it is necessary to force manually correct mill binary version for the plugin[^1].

:::

```scala
import $ivy`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin_mill0.12:@VERSION@`
```

By default, the plugin will look for Smithy files under the `$MY_MODULE/smithy` directory. The generated code ends up in `out/$MY_MODULE/smithy4sOutputDir.dest/scala/`, again, by default. Code generation happens automatically before you `compile` the module. The paths are configurable via the `smithy4sInputDirs` and `smithy4sOutputDir` tasks.

For example, here we'll read Smithy files from `smithy_input` and write to `smithy_output`.

```scala
import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.18"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )

  override def smithy4sInputDirs = T.sources {
    Seq(PathRef(T.ctx().workspace / "smithy_input"))
  }
  override def smithy4sOutputDir = T {
    PathRef(T.ctx().workspace / "smithy_output")
  }
}
```

[^1]: https://github.com/com-lihaoyi/mill/issues/4390

### Mill 1.x

In your `build.mill`:

```scala
//| mvnDeps:
//| - com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@

package build

import mill.*, mill.scalalib.*
import smithy4s.codegen.mill.*

object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.18"
  override def mvnDeps = Seq(
    mvn"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )
}
```

By default, the plugin will look for Smithy files under the `$MY_MODULE/smithy` directory. Code generation happens automatically before you `compile` the module.

To customize input and output directories:

```scala
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.18"
  override def mvnDeps = Seq(
    mvn"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}"
  )

  override def smithy4sInputDirs = Task.Sources("smithy_input")
  override def smithy4sOutputDir = Task {
    PathRef(moduleDir / "smithy_output")
  }
}
