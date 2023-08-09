---
sidebar_label: Wildcard types
title: Scala wildcard type arguments
---

Scala has a specific syntax for wildcard argument in types. In Scala 2, that was the underscore: `_`. But with Scala 3, this is changing. See [the language reference page](https://docs.scala-lang.org/scala3/reference/changed-features/wildcards.html) for more information.

Smithy4s now has a way for you to control that, and the good thing is that you probably don't have to worry about it. If you're using Smithy4s via `mill` or `sbt`, then it's taken care of you. It can be overriden via the following keys:

* in mill, task: `def smithy4sWildcardArgument = "?" // or "_"`
* in sbt, setting: `smithy4sWildcardArgument := "?" // or "_"`

If you are using Smithy4s via the CLI, then they way to utilize this feature is through your Smithy specifications. The simplest approach is to add a file with the following content to your CLI invocation:

```smithy
$version: "2"

metadata smithy4sWildcardArgument = "?" // can also be `_`
```