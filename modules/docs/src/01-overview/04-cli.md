---
sidebar_label: Installation (CLI)
title: Installation (CLI)
---

Beside the provided sbt plugin, smithy4s can be used as a CLI. It allows generating Scala code and OpenAPI specs from smithy specs.

We recommend using [coursier](https://get-coursier.io/docs/cli-launch) to install/run it.

### Installation

```bash
cs install --channel https://disneystreaming.github.io/coursier.json smithy4s
```

### Usage

```scala mdoc:invisible
import com.monovore.decline._
import smithy4s.codegen.cli._
```

The CLI comes with a number of options to customize output directories, skip openapi generation (or scala generation), provide a filter of allowed namespaces, etc. Use the `--help` command to get an exhaustive listing.

```scala mdoc:passthrough
println("```bash")
println("bash> smithy4s --help")
println(smithy4s.codegen.cli.Main.commands.showHelp)
println("```")
```

#### Codegen

```scala mdoc:passthrough
println("```bash")
println("bash> smithy4s generate --help")
println(Help.fromCommand(CodegenCommand.command))
println("```")
```

#### Dump model

```scala mdoc:passthrough
println("```bash")
println("bash> smithy4s dump-model --help")
println(Help.fromCommand(DumpModelCommand.command))
println("```")
```
