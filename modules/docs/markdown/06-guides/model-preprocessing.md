---
sidebar_label: Model preprocessing
title: Smithy Model preprocessing
---

There are times that you may want to transform the Smithy model being used by Smithy4s prior to code generation. This happens often when the model in question is provided by a third party: you may only be interested in a couple operations from a third party service, or you may want to remove some fields that are irrelevant for your use-case from of a response,
in order to reduce the parsing overhead.

In this guide, we will walk through exactly how you can accomplish this. As an example, we will show how you can remove members marked with a certain trait from structures inside of your Smithy model. However, you can use the same principles in this guide to accomplish whatever other transformations you may need.

## Starting Smithy Model

```smithy
namespace preprocessed

@trait(selector: "structure > member")
structure removeBeforeCodegen {}

structure MyStruct {
    @required
    name: String

    @removeBeforeCodegen
    id: String
}
```

Here we have defined a trait `removeBeforeCodegen`. We have marked the `id` member of `MyStruct` with this trait. As such, we will implement a transformer which will lead to the model looking as follows:

```smithy
namespace preprocessed

structure MyStruct {
    @required
    name: String
}
```

This is the model that will ultimately be fed into the Smithy4s code generation tooling.

## Note on third party models

It is likely that you will want to annotate third party models. Remember that Smithy allows for annotating shapes
with traits a posteriori, via the following syntax :

```smithy
apply preprocessed#MyStruct$id @removeBeforeCodegen
```

This lets you regain control over models that came from third party before running the code-generation.

## Create a new build project/module to hold a ProjectionTransformer

A model preprocessor is essentially an implementation of the `software.amazon.smithy.build.ProjectionTransformer`
interface, provided by the official `smithy-build` library. This code is leveraged at build-time, and it is unlikely
something that developers want in the runtime classpath of their application. Therefore, a bespoke project/module
must be created to hold the implementation.

### SBT

In our `build.sbt` file we will create a new project called `preprocessors` that looks as follows:

```scala
lazy val preprocessors = (project in file("preprocessors"))
  .settings(
    scalaVersion := "2.12.13", // 2.12 to match what SBT uses
    name := "preprocessors",
    libraryDependencies += "software.amazon.smithy" % "smithy-build" % smithy4s.codegen.BuildInfo.smithyVersion
  )
```

### Mill

```scala
import mill._
import scalalib._

object preprocessors extends ScalaModule {
  def scalaVersion = "2.13.10" // 2.13 to match what Mill uses
  def ivyDeps = Agg(
    s"software.amazon.smithy:smithy-build:${smithy4s.codegen.BuildInfo.smithyVersion}"
  )
}
```

## Implement the ProjectionTransformer

Here is an example of a transformer that will remove the members marked with the `removeBeforeCodegen`
trait as discussed above.

Note that the result of the `getName` method is significant, as it will be referenced in the build later,
but it does not have to match the name of the class.

```scala
package preprocessors

import software.amazon.smithy.build._
import software.amazon.smithy.model._
import software.amazon.smithy.model.shapes._
import software.amazon.smithy.model.traits._

final class RemoveBeforeCodegenTransformation extends ProjectionTransformer {

  def getName() = {
    "RemoveBeforeCodegenTransformation"
  }

  def transform(ctx: TransformContext) : Model = {
    val toRemove = ctx
      .getModel()
      .getShapesWithTrait(ShapeId.from("preprocessed#removeBeforeCodegen"))

    ctx.getTransformer().removeShapes(ctx.getModel(), toRemove)
  }
}
```

Inside the `transform` method we remove all shapes that are marked with the `removeBeforeCodegen` trait, before returning
the final model.

## Register the Transformer

We need to register the Transformer so that the Smithy tooling is be able to find it when necessary. We do this by creating the following file :

* for SBT : `src/main/resources/META-INF/services/software.amazon.smithy.build.ProjectionTransformer`
* for Mill : `resources/META-INF/services/software.amazon.smithy.build.ProjectionTransformer`

This file contains a list of newline-delimited fully qualified names, of all the `ProjectionTransformer` implementations contained by our project. For our use-case, it looks like this :

```
preprocessors.RemoveBeforeCodegenTransformation
```

NB : this registration is dictated by the [**Service Provider Interface**](https://www.baeldung.com/java-spi) (aka **SPI**). It is the same mechanism that the Scala compiler uses to find compiler plugins from the classpath.

## Wire up

Now, we need to indicate to the smithy4s build plugin which transformers should be applied prior to code generation in our application project. We also need to wire the `preprocessors` project/module to our application project in a way that ensures the transformer does not end up in the runtime classpath of the application.

### SBT

Now, in our project that is using the Smithy4s SBT plugin (`.enablePlugins(Smithy4sCodegenPlugin)`) we need to add the following settings:

```scala
lazy val app = project.in(file("app"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    // ...

    // Must match the `getName` method implemented above
    Compile / smithy4sModelTransformers += "RemoveBeforeCodegenTransformation",
    Compile / smithy4sAllDependenciesAsJars += (transformers / Compile / packageBin).value
  )
```

### Mill

```scala
object app extends Smithy4sModule {
  // ...

  def smithy4sModelTransformers = T {
    List(
      // Must match the `getName` method implemented above
      "RemoveBeforeCodegenTransformation"
    )
  }

  def smithy4sAllDependenciesAsJars = T {
     preprocessors.jar() :: super.smithy4sAllDepencenciesAsJars()
  }

}
```

## Outcome

This results in the generated `MyStruct` case class to look like this :

```scala
// note the lack of the `id` field which was removed by the preprocessor
case class MyStruct(name: String)
```

of course, this is but an example, but some models contain thousands of shapes. Automating the preprocessing
of these models is extremely powerful.

## Directory Structure

In case the directory and file structure above was hard to follow, here is a tree example of what it would look like for this example:

### SBT

```
├── build.sbt
├── app
│   └── src
│       └── main
│           ├── scala
│           │   └── com
│           │       └── example
│           │           └── Main.scala
│           └── smithy
│               └── preproccessed.smithy // The first smithy snippet shown above
├── project
│   ├── build.properties
│   └── plugins.sbt
└── preprocessors
    └── src
        └── main
            ├── resources
            │   └── META-INF
            │       └── services
            │           └── software.amazon.smithy.build.ProjectionTransformer // The file which registers our ProjectionTransformer
            └── scala
                └── preprocessors
                    └── RemoveBeforeCodegenTransformation.scala // The ProjectionTransformer
```

### Mill

```
├── build.sc
├── app
│   ├── src
│   │   └── com
│   │       └── example
│   │           └── Main.scala
│   └── smithy
│       └── preproccessed.smithy // The first smithy snippet shown above
└── preprocessors
    └── src
    │   └── preprocessors
    │       └── RemoveBeforeCodegenTransformation.scala // The ProjectionTransformer
    ├── resources
    │   └── META-INF
    │       └── services
    │           └── software.amazon.smithy.build.ProjectionTransformer // The file which registers our ProjectionTransformer
```
