[![CI](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml/badge.svg)](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml)
[![smithy4s-core Scala version support](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core/latest-by-scala-version.svg)](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core)
[![CLA assistant](https://cla-assistant.io/readme/badge/disneystreaming/smithy4s)](https://cla-assistant.io/disneystreaming/smithy4s)

# smithy4s

## Usage

**For usage information, check out the [Documentation](https://disneystreaming.github.io/smithy4s/)**

## Benchmarks

There's a module with benchmarks to compare against the handcrafted implementations of an http4s with the generic ones run the benchmarks, one should execute:

```sh
sbt benchmark/jmh:run
```

To run http4s benchmarks:

```sh
benchmark/jmh:run .*Http4sBenchmark.*
```

To benchmark with Scala 2.12 version instead of 2.13, use `benchmark2_12/jmh:run` instead.
