[![CI](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml/badge.svg)](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml)
[![smithy4s-core Scala version support](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core/latest-by-scala-version.svg)](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core)
[![CLA assistant](https://cla-assistant.io/readme/badge/disneystreaming/smithy4s)](https://cla-assistant.io/disneystreaming/smithy4s)

# smithy4s

## Usage

**For usage information, check out the [Documentation](https://disneystreaming.github.io/smithy4s/)**

## Benchmarks

There's a module with benchmarks to compare against the handcrafted implementations of an http4s with the generic ones run the benchmarks, one should execute:

```sh
sbt benchmark / Jmh / run
```

To run http4s benchmarks:

```sh
benchmark / Jmh / run .*Http4sBenchmark.*
```

To benchmark with Scala 2.12 version instead of 2.13, use `benchmark2_12 / Jmh / run` instead.

Smithy4s makes use of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) for performance optimisation.<br/>
![YourKit Logo](https://www.yourkit.com/images/yklogo.png)

## Development environment

For the easiest setup of development tools, use [Nix](https://nixos.org).

The recommended way is to use `nix develop` (requires Flakes support, available since Nix 2.4 - read on if you don't use that experimental feature):

```bash
nix develop
```

This will load all required packages into your shell. Run `exit` or press `ctrl+d` to clear it.

If you're a [direnv](https://github.com/nix-community/nix-direnv) user, we have that too.

If you don't have Flakes support:

```bash
nix-shell
```
