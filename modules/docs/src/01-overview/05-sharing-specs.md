---
sidebar_label: Sharing specifications
title: Sharing specifications
---

The core Smithy tooling built by AWS makes it easy to load Smithy files that are packaged in jars. Smithy4s makes use of this feature to allow users
to generate code from specs that may be found in Maven Central or other artifact repository.
# Packaging specifications in order to share them
## Scala-agnostic context

If you work in a context that is not primordially Scala-centric, you may want to package Smithy specification in Jars to make them easily accessible to various
code-generator tools. When that is the case, it is not-advised to use Smithy4s in order to package specifications, as the consuming applications/tools might not
have awareness of Scala. The best practice would likely be to have jars that would contain only Smithy files and potentially pure-java custom validators.

In order to package Smithy files in jars so that they can be easily consumed by tools, here are the core details:

1. All smithy files should be stored under `src/main/resources/META-INF/smithy/` (or in another resource directory, under `META-INF/smithy`)
2. A `manifest` file should be stored under that same directory
3. The `manifest` file should reference all the smithy files that can be found in that `META-INF/smithy` directory.
4. If you are using SBT to do this, consider setting `autoScalaLibrary := false`. See [here](https://www.scala-sbt.org/1.x/docs/Configuring-Scala.html#Configuring+the+scala-library+dependency) for more information.
5. If you are using Mill to do this, consider using a `JavaModule` instead of a `ScalaModule`.

A couple examples:

* [smithy-aws-apigateway-traits](https://github.com/awslabs/smithy/tree/main/smithy-aws-apigateway-traits/src/main/resources/META-INF/smithy)
* [smithy4s-protocols](https://github.com/disneystreaming/smithy4s/tree/main/modules/protocol/resources/META-INF/smithy)

## Smithy4s-context

The Smithy4s build-plugins we provide out of the box automatically package the local specifications (used for code-generations) in the resulting jars so that downstream projects (internal and external) can use them. When doing so, Smithy4s abides by the same structure described above.

Additionally, Smithy4s will also produce a smithy file containing a piece of metadata listing the namespaces for which code was generated. This way, downstream Smithy4s calls can automatically skip the already-generated namespaces.

This does mean two things:

1. Users do not have to manually indicate namespaces that were already generated.
2. When using multi-module builds, Smithy specifications in one module can depend on Smithy specifications in another module it depends on, without the user having to do anything bespoke for it. The resulting Scala code in the downstream module will simply depend on the one in the upstream module, as if it had been handwritten.

### Disabling packaging of smithy files in jars

If for some reason you want to disable the packaging of Smithy files in the jars created by your build tool, follow the instructions below.

#### SBT

Add the following setting to your project

```scala
Compile / smithy4sSmithyLibrary := false
```

#### Mill

Override the following method in your module

```scala
override def smithy4sSmithyLibrary = T(false)
```

### Disabling the dependency on smithy files in sibling projects

If your project has a multi-module build and some of the modules have the plugin enabled,
due to the behavior documented above, dependencies will need to be compiled before code can be generated.

Consider the following build (sbt syntax):

```
val a = project
val b = project.enablePlugin(Smithy4sCodegenPlugin).dependsOn(a)
```

Whenever you want to generate the Scala code in project `b`, your build tool will trigger compilation of `a`. This happens so that the Smithy files in the `a` project get packaged into a `.jar` file - just like they normally are when you package the `a` project otherwise (for `publishLocal`, `stage` etc.).

You can opt out of this behavior:

#### SBT

```scala
val b = project.settings(
  Compile / smithy4sAggregateLocalDependencies := false
)//...
```

#### Mill

```scala
object b extends Smithy4sModule {
  //...
  override def smithy4sLocalJars = List.empty[PathRef]
}
```

This will not only remove the need for compilation (for the purposes of codegen), but also remove any visibility of the Smithy files in the **local** dependencies of your project (**local** meaning they're defined in the same build)


### A word of warning

Smithy4s optimises for "correctness" as opposed to "compatibility." This means the generated Scala code aims at 1) being an accurate reflection of the Smithy models and 2) providing an idiomatic developer experience. This happens at the cost of a lack of guarantees around the binary compatibility of the generated code when the Schema evolves.

When packaging Smithy specs in artifacts that contain Smithy4s-generated code, developers should keep that aspect in mind, and ensure that the version of Smithy4s that produced upstream artifacts is binary-compatible with the version that they use locally. Tools such as MiMa can help

We cannot recommend treating Smithy4s-generated code as publishable library-material. Should you decide to do so, please exercise caution.

# Depending on shared specifications
## Artifacts containing only specifications

For instance, AWS publishes a number of [api-gateway specific traits](https://github.com/awslabs/smithy/tree/main/smithy-aws-apigateway-traits/src/main/resources/META-INF/smithy) to [Maven central](https://search.maven.org/artifact/software.amazon.smithy/smithy-aws-apigateway-traits) (the shapes are defined there in a smithy-compliant Json file).

## SBT

Using the SBT plugin, the `Smithy4s` config object can be used to tag dependencies that Smithy4s should feed to the code generator.

You can declare your intent to depend on these smithy definitions as such:

```scala
libraryDependencies += "software.amazon.smithy" % "smithy-aws-iam-traits" % "1.14.1" % Smithy4s
```

## Mill

Mill uses a separate task to define dependencies that the code-generator should have awareness of:

```scala
def smithy4sIvyDeps = Agg(ivy"software.amazon.smithy::smithy-aws-iam-traits:1.14.1")
```
## Consequence

This will have the effect of loading the contents of the smithy files (or smithy-compliant Json files) from the artifact into the aggregated model that Smithy4s uses as an input to the code generator. It means that the traits and shapes defined in these files will be available to use in your models, but it also means that Smithy4s will try to generate code for these shapes.

This artifact will not be included as a dependency to your project at compile-time (nor runtime), it will only be consumed for the
Smithy specs (and validators) it may contain.

## Artifacts containing both Smithy files and Smithy4s generated code

When using Smithy4s, you may want to depend on artifacts that may have been built using Smithy4s, containing both Smithy specifications
and generated Scala code (or rather, JVM bytecode resulting from the compilation of generated Scala code). In this case, you have to tell your build tool that a dependency should be used both by Smithy4s at codegen-time, and by the Scala compiler at compile time. This is achieved by doing the following

### SBT

```scala
libraryDependencies += "organisation" % "artifact" % "version" % Smithy4sCompile
```

Which is merely a shortcut for:

```scala
libraryDependencies += "organisation" % "artifact" % "version" % "smithy4s,compile"
```

### Mill

```scala

def compileAndCodegenDeps = T(Agg(ivy"organisation:artifact:version"))
def ivyDeps = T(super.ivyDeps() ++ compileAndCodegenDeps())
def smithy4sIvyDeps = T(super.smithy4sIvyDeps() ++ compileAndCodegenDeps())

```

### Consequence

Because the upstream usage of Smithy4s will have resulted in the creation of metadata tracking the namespaces that were already generated, the "local" Smithy4s code-generation will automatically skip the generation of code that should not be generated again.


### Manually skipping (or including) namespaces during code-generation.

Sometimes, you may want to tell Smithy4s to skip code-generation of some namespaces altogether, because the corresponding code may have been produced by another tool than Smithy4s. In that case, you can gain control over which namespaces Smithy4s crawls through when performing the code generation to avoid regenerating code that already exists. This is achieved via a couple of build-settings (the names are shared between SBT and Mill).

* `smithy4sAllowedNamespaces` which is an allow-list
* `smithy4sExcludedNamespaces` which is a disallow-list

By default, Smithy4s tries to generate everything but shapes that are in the following namespaces:

* `smithy4s.api`
* `smithy4s.meta`
* namespaces that start with `aws`
* namespaces that start with `smithy`

### Note regarding credentials

The SBT plugin provided by Smithy4s uses SBT's resolution mechanism (based on coursier) to retrieve the artifacts from their respective repositories. This implies that the resolvers-related settings are respected, included credentials that may be needed to read from some private artifact repository.

In the CLI, the [mechanisms native to coursier](https://get-coursier.io/docs/2.0.0-RC4-1/other-credentials#docsNav) are respected.
