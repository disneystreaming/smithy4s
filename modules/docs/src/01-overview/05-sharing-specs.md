---
sidebar_label: Sharing specifications
title: Sharing specifications
---

The core Smithy tooling built by AWS makes it easy to load Smithy files that are packaged in jars. Smithy4s makes use of this feature to allow users
to generate code from specs that may be found in Maven Central or other artifact repository.

# Depending on shared specifications

## SBT

Using the SBT plugin, the `Smithy4s` config object can be used to tag dependencies that Smithy4s should feed to the code generator.
For instance, AWS publishes a number of [api-gateway specific traits](https://github.com/awslabs/smithy/tree/main/smithy-aws-apigateway-traits/src/main/resources/META-INF/smithy) to [Maven central](https://search.maven.org/artifact/software.amazon.smithy/smithy-aws-apigateway-traits/1.21.0/jar) (the shapes are defined there in a smithy-compliant Json file).

You can declare your intent to depend on these smithy definitions as such :

```scala
libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % "1.14.1" % Smithy4s
```

This will have the effect of loading the contents of the smithy files (or smithy-compliant Json files) from the artifact into the aggregated model that Smithy4s uses as an input to the code generator. It means that the traits and shapes defined in these files will be available to use in your models, but it also means that Smithy4s will try to generate code for these shapes.

It sometimes happens that Scala code reflecting these shapes was already generated and made available to another module or artifact. In that case, you can gain control over which namespaces Smithy4s crawls through when performing the code generation to avoid regenerating code that already exists. This is achieved via a couple of SBT settings :

* `smithy4sAllowedNamespaces` which is an allow-list
* `smithy4sExcludedNamespaces` which is a disallow-list

By default, Smithy4s tries to generate everything but shapes that are in the following namespaces :

* `smithy4s.api`
* `smithy4s.meta`
* namespaces that start with `aws`
* namespaces that start with `smithy`

### Note regarding credentials

The SBT plugin provided by Smithy4s uses SBT's resolution mechanism (based on coursier) to retrieve the artifacts from their respective repositories. This implies that the resolvers-related settings are respected, included credentials that may be needed to read from some private artifact repository.

In the CLI, the [mechanisms native to coursier](https://get-coursier.io/docs/2.0.0-RC4-1/other-credentials#docsNav) are respected.

## Packaging specifications in order to share them

Smithy4s does not have any features that help in the packaging of specifications. This is a design decision that can be explained by the fact that Smithy4s
optimises for "correctness" as opposed to "compatibility." This means the generated Scala code aims at 1) being an accurate reflection of the Smithy models and 2) providing an idiomatic developer experience. This happens at the cost of a total lack of guarantees around the binary compatibility of the generated code when the Schema evolves.

Therefore, we recommend that you _DO NOT_ treat the generated code as publishable library-material. Additionally, the inlined smithy files that you may have under `src/main/smithy` are **not packaged** as resources by Smithy4s.

However, if you want to package specifications in jars and publish them to an artifact repository so that several codebases may benefit from it (via Smithy4s or other tools), it is really simple:

1. All smithy files should be stored under `src/main/resources/META-INF/smithy/` (or in another resource directory, under `META-INF/smithy`)
2. A `manifest` file should be stored under that same directory
3. The `manifest` file should reference all the smithy files that can be found in that `META-INF/smithy` directory.

A couple examples :

* [smithy-aws-apigateway-traits](https://github.com/awslabs/smithy/tree/main/smithy-aws-apigateway-traits/src/main/resources/META-INF/smithy)
* [smithy4s-protocols](https://github.com/disneystreaming/smithy4s/tree/main/modules/protocol/resources/META-INF/smithy)
