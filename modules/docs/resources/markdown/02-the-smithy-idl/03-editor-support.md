---
sidebar_label: Editor support
title: Editor Support
---

Disney Streaming develops and maintains a [Smithy language server](https://github.com/disneystreaming/smithy-language-server), that implements features such as jump-to-definition, auto-completion, validation diagnostics.

We also provide a [VS Code extension](https://marketplace.visualstudio.com/items?itemName=disneystreaming.smithy) that talks to the language server, and provides a smooth developer experience.

## Configuration

You can configure your language server using a `smithy-build.json` at the root of the workspace. The smithy4s plugins have a task to generate that file according to your modules configuration.

For sbt: `sbt smithy4sUpdateLSPConfig` and mill: `mill smithy4s.codegen.LSP/updateConfig`.

If you already have a file, it will merge the existing file with the generated configuration.