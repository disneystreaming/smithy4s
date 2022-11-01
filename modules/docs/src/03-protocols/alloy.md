# Alloy

Throughout the smithy4s documentation and its various protocols, you are likely to run into the term `alloy` at some point. Alloy is an open-source set of Smithy shapes that we have published and use across our projects.

## What does Alloy contain?

Alloy is a place that contains generic Smithy shapes that are commonly used across multiple projects. This includes things such as `UUID`, `untagged`, `discriminated`, etc.

See [Alloy documentation](https://github.com/disneystreaming/alloy) for more information about the shapes it contains.

## Why Alloy?

As mentioned above, having a common place where we publish shapes reduces the amount of duplication across our different projects (such as smithy4s and [smithy-translate](https://github.com/disneystreaming/alloy)). Further, having more common shapes reduces the number of transformations that need to be done when using shapes in various projects. This simplifies the development efforts of anyone involved with creating smithy-based tooling.

## Migration from smithy4s.api

Alloy was introduced in Q4 of 2022. Prior to this, smithy4s used its own protocol called `smithy4s.api#simpleRestJson`. Migrating from `smithy4s.api#simpleRestJson` to `alloy#simpleRestJson` should be trivial. The shapes have retained their semantics and use the same validation as prior. You should be able to just change `smithy4s.api` to `alloy` in your Smithy files and be good to go. If you run into any issues, reach out to us in GitHub discussions or issues.
