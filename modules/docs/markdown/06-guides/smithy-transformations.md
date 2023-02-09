---
sidebar_label: Smithy Transformations
title: Smithy Transformations
---

There are times that you may want to transform the Smithy model being used by Smithy4s prior to code generation. In this guide, we will walk through exactly how you can accomplish this. As an example, we will show how you can remove members marked with a certain trait from structures inside of your Smithy model. However, you can use the same principles in this guide to accomplish whatever other transformations you may need.

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

## Implement the Transformer

Now we will walk through the steps of actually implementing the Smithy model transformer. The `smithy-build` Java library provides an interface for these called `ProjectionTransformer`. Here is an example of a transformer that will remove the members marked with the `removeBeforeCodegen` trait as discussed above.

```scala
final class RemoveBeforeCodegenTransformation extends ProjectionTransformer {
  def getName() = {
    "RemoveBeforeCodegenTransformation"
  }

  def transform(x: TransformContext) = {
    val toRemove = x
      .getModel()
      .getShapesWithTrait(new ToShapeId() {
        override def toShapeId(): ShapeId =
          ShapeId.from("preprocessed#removeBeforeCodegen")
      })
    x.getTransformer().removeShapes(x.getModel(), toRemove)
  }
}
```

Here we create a `ProjectionTransformer` named `RemoveBeforeCodegenTransformation`. Inside the `transform` method we remove all shapes that are marked with the `removeBeforeCodegen` trait and return the resultant model.

## Register the Transformer

We need to register the Transformer so that the Smithy tooling will be able to find it when it does a lookup for all `ProjectionTransformer` implementations. We do this by creating a file at `src/main/resources/META-INF/services/` called `software.amazon.smithy.build.ProjectionTransformer`. Inside of this file, we need to place the fully qualified classpath that leads to our transformer (e.g. `preprocessed.transformations.RemoveBeforeCodegenTransformation`).

## Wire up in SBT

In our `build.sbt` file we will create a new project called `transformers` that looks as follows:

```scala
lazy val transformers = (project in file("transformers"))
  .settings(
    name := "transformers",
    libraryDependencies += "software.amazon.smithy" % "smithy-build" % smithy4s.codegen.BuildInfo.smithyVersion
  )
```

This project is where our `RemoveBeforeCodegenTransformation` will live. As such, it requires the `smithy-build` dependency so it can access the `ProjectionTransformer` interface.

Now, in our project that is using the Smithy4s SBT plugin (`.enablePlugins(Smithy4sCodegenPlugin)`) we need to add the following settings:

```scala
...
.enablePlugins(Smithy4sCodegenPlugin)
.settings(
  ...
  // transformers
  Compile / smithy4sModelTransformers += "RemoveBeforeCodegenTransformation",
  Compile / smithy4sAllDependenciesAsJars += (transformers / Compile / packageBin).value
)
```

Here we are telling the smithy4s SBT plugin which transformers should be applied prior to code generation. In this case, "RemoveBeforeCodegenTransformation" is the only one. From there, we are telling the plugin that it should use the transformers SBT project we created above as a dependency. Adding it this way will make sure that your transformer is NOT added as a dependency to anything other than the Smithy4s plugin.

## Directory Structure

In case the directory and file structure above was hard to follow, here is a tree example of what it would look like for this example:

```
├── build.sbt
├── core
│   └── src
│       └── main
│           ├── scala
│           │   └── com
│           │       └── example
│           │           └── Main.scala
│           └── smithy
│               └── preproccessed.smithy // The first smithy snippet shown above
├── project
│   ├── build.properties
│   └── plugins.sbt
└── transformers
    └── src
        └── main
            ├── resources
            │   └── META-INF
            │       └── services
            │           └── software.amazon.smithy.build.ProjectionTransformer // The file which registers our ProjectionTransformer
            └── scala
                └── preprocessed
                    └── transformations
                        └── RemoveBeforeCodegenTransformation.scala // The transformer
```
