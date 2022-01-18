# CLI

Beside the baked-in SBT plugin, smithy4s comes with a CLI, that allows to generate Scala code and OpenAPI specs from smithy specs.

We recommend using [coursier](https://get-coursier.io/docs/cli-launch) to install/run it

#### Installation

cs install --channel https://disneystreaming.github.io/coursier.json smithy4s

#### Usage

```bash 
bash> smithy4s generate ./foo.smithy ./bar.smithy
```

The CLI comes with a number of options to customize output directories, skip openapi generation (or scala generation), provide a filter of allowed namespaces, etc. Use the `--help` command to get an exhaustive listing.
