---
sidebar_label: Smithy4s to Smithy
title: Converting Smithy4s Schemas and Services to Smithy
---

Using the smithy4s dynamic module you can convert a smithy4s service or schema into a smithy model. This guide will walk through the steps to do this.

## Creating a DynamicSchemaIndex

The first step is to take the services and schemas you'd like included in your smithy model and add them to a DynamicSchemaIndex using the provided builder.

```scala mdoc
import smithy4s.dynamic.DynamicSchemaIndex

val dynamicSchemaIndex = DynamicSchemaIndex.builder
  .addService[smithy4s.example.KVStoreGen]
  .addSchema[smithy4s.example.FaceCard]
  .build()
```

## Converting to Smithy Model

Now that we have a DynamicSchemaIndex, we can convert to a smithy model object from the smithy-model Java library. This feature is only supported on the JVM and not ScalaJS or Scala Native.

```scala mdoc
val model = dynamicSchemaIndex.toSmithyModel
```

## Rendering as a String

If you wish to render the smithy `Model` as a String, smithy-model provides a method to accomplish this.

```scala mdoc
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer
import scala.jdk.CollectionConverters._
import java.nio.file.Path

val smithyFiles: Map[Path, String] = 
  SmithyIdlModelSerializer
      .builder()
      .build()
      .serialize(model)
      .asScala
      .toMap
```
