---
sidebar_label: Validated Newtypes
title: Validated Newtypes
---

As of version `0.19.x` Smithy4s has the ability to render constrained newtypes over Smithy primitives as
"validated" classes in the code it generates. In practice this means that a newtype will now have an 
`apply` method that returns either a validated value or an error.

The way to utilize this feature is through your Smithy specifications by adding a file with the following 
content to your Smithy sources:

```kotlin
$version: "2"

metadata smithy4sRenderValidatedNewtypes = true
```

Alternatively, if you want to generate validated newtypes only for select shapes in your model, you can accomplish
this using the `smithy4s.meta#validateNewtype` trait. This trait can only be used on number shapes with a range
constraint or string shapes with pattern and/or length constraints.

```kotlin
use smithy4s.meta#validateNewtype

@validateNewtype
@length(min: 5)
string Name
```

Below is the generated scala class that Smithy4s will generate:

```scala mdoc:compile-only

import smithy4s._
import smithy4s.schema.Schema.string

type Name = Name.Type

object Name extends ValidatedNewtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "Name")

  val hints: Hints = Hints.empty

  val underlyingSchema: Schema[String] = 
    string
      .withId(id)
      .addHints(hints)
      .validated(smithy.api.Length(min = Some(5L), max = None))

  val validator: Validator[String, Name] = 
    Validator.of[String, Name](Bijection[String, Name](_.asInstanceOf[Name], value(_)))
      .validating(smithy.api.Length(min = Some(5L), max = None))

  implicit val schema: Schema[Name] = validator.toSchema(underlyingSchema)

  @inline def apply(a: String): Either[String, Name] = validator.validate(a)
}
```