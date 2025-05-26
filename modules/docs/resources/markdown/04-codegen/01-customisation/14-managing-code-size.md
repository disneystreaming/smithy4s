---
sidebar_label: Managing code size
title: Managing code size
---

As we currently do not have plans to publish pre-compiled AWS client artifacts,
users are advised to generate the code for services they use as part of their own build setup.

While the process is streamlined, it immediately presents a unique challenge - the amount of generated
code is quite large even for moderately sized AWS services, it can run into hundreds of files for various
operations, data structures, and newtypes.

Incremental compilation available in all Scala build tools generally handles this well: after compiling the mountain of files once, you are unlikely to ever have to recompile them again, unless you explicitly clean the cache.

But there are situations where incremental compilation is not available – for example, on CI, or if building a deployment artifact from scratch. In those situations this amount of code can become problematic.

Additionally, smaller code size in a Smithy4s application has a positive effect on application startup time, and allows you to build smaller, leaner JARs. With Scala Native and Scala.js this will also result in smaller binary/bundle sizes.

There are two possible solutions to this:

1. Pre-build the AWS artifacts for the services you use, and publish them to your organisation's internal artifact registry
2. Limit the amount of code being generated to the subset of service operations your project actually uses

Option 1 hopefully doesn't need much explanation - once the code has been generated, it can be published as a regular Scala artifact into a registry of your choice, such as JFrog Artifactory.

Option 2 is what we will focus on here, as it allows you to drastically reduce code size as part of your own project, without having to publish anything anywhere.

To demonstrate this approach better, let's immediately start with an example.

## Example

Let's say you decided to build a service that uses [AWS Comprehend](https://docs.aws.amazon.com/comprehend/latest/dg/sdk-general-information-section.html) to detect sentiment in text provided by the user.

You start with the [smithy4s.g8 template](https://github.com/disneystreaming/smithy4s.g8/), and, following the [AWS support documentation](../../03-protocols/03-aws/01-aws.md), instruct Smithy4s plugin to generate you a client SDK for Comprehend:

```scala
    smithy4sAwsSpecs ++= Seq(AWS.comprehend)
```

As you run `compile` in your build tool, you can see that there are 428 files being compiled. Uh-oh.

Those 428 files cover the entirety of Comprehend's [84 operations](https://docs.aws.amazon.com/comprehend/latest/APIReference/API_Operations.html) and all the datatypes supporting them. But we don't need all of these operations - we only care about [DetectSentiment](https://docs.aws.amazon.com/comprehend/latest/APIReference/API_DetectSentiment.html).

### `@only` annotation

To express exactly that, Smithy4s ships with a built-in annotation `smithy4s.meta#only`, which can be applied to operations (and **operations only**) that you would like to keep in generated code, along with all the other Smithy shapes they reference.

The exact semantics of the annotation are as follows:

- It can only be applied to operations
- For any service that has operations tagged with this annotation, the rest of the operations will be **removed**, along with all the shapes (structures, aliases, unions, enums, etc.) that those operations reference, as long as those shapes are deemed **removable** (see below)
- A shape is deemed **removable** if it's only reachable from the removed operations (either directly or transitively)
- Services that don't have *any* operations tagged with this annotation are assumed to have *all* of their operations tagged implicitly (so that all the operations are kept)

The key mechanism that allows this operation to be usable with AWS client generation is that we can [apply annotations post factum on shapes defined elsewhere](https://disneystreaming.github.io/smithy4s/docs/guides/model-preprocessing#note-on-third-party-models).

So to only preserve `DetectSentiment` operation, we can create a Smithy file in our project with these contents:

```smithy
$version: "2"

namespace my.code

use smithy4s.meta#only

apply com.amazonaws.comprehend#DetectSentiment @only
```

The namespace here doesn't matter. `com.amazonaws.comprehend#DetectSentiment` is a fully qualified name referring to the `DetectSentiment` operation in AWS Comprehend.

If we re-run the code generation, it will only produce 15 files! This will dramatically speed up the compilation and improve the JAR size.

You can apply this annotation to as many operations as you want:

```smithy
$version: "2"

namespace my.code

use smithy4s.meta#only

apply com.amazonaws.comprehend#DetectSentiment @only
apply com.amazonaws.comprehend#CreateEntityRecognizer @only
apply com.amazonaws.comprehend#CreateFlywheel @only
// etc.
```

