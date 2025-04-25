[![CI](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml/badge.svg)](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml)
[![smithy4s-core Scala version support](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core/latest-by-scala-version.svg)](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core)
[![CLA assistant](https://cla-assistant.io/readme/badge/disneystreaming/smithy4s)](https://cla-assistant.io/disneystreaming/smithy4s)
[![Discord](https://img.shields.io/discord/1045676621761347615.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/wvVga94s8r)
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

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md)
