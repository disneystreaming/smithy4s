---
sidebar_label: Open Enums
title: Open Enumerations
---

By default, `enum` and `intEnum` shapes are considered to be closed and they are rendered as such. This means that it is expected that all possible values that can be in the enum are declared in the Smithy specification. However, there are certain times where you may require an open enumeration, meaning that values can be placed into it which are not declared in the Smithy specification. This can be useful for interoperability with APIs that you don't control, although often times a simple `String` or `Integer` shape will better suit a field where the values are not known beforehand.

Open enumerations can be specified using the `alloy#openEnum` trait ([docs here](https://github.com/disneystreaming/alloy#alloyopenenum)).

```kotlin
use alloy#openEnum

@openEnum
enum Shape {
  SQUARE, CIRCLE
}

@openEnum
intEnum IntShape {
  SQUARE = 1
  CIRCLE = 2
}
```

When the `alloy#openEnum` trait is present, it makes it so the enumeration is rendered in the generated code with an extra case, `$Unknown`. For Example:

```scala
case object SQUARE extends Shape
case object CIRCLE extends Shape
final case class $$Unknown(value: String) extends Shape
```

Note that the leading `$$` is added onto the `Unknown` case to prevent potential collisions with actual enum values.
