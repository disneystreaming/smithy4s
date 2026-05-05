---
sidebar_label: Unpacked outputs
title: Unpacked outputs
---

By default, Smithy4s generates methods whose return type is the output structure of the corresponding operation.

For instance:

```kotlin
service ItemService {
  version: "1.0.0"
  operations: [GetItem]
}

operation GetItem {
  output: GetItemOutput
}

structure GetItemOutput {
    @required
    item: Item
}

structure Item {
    @required
    id: String
}
```

leads to something conceptually equivalent to:

```scala
trait ItemServiceGen[F[_]] {

  def getItem(): F[GetItemOutput]

}
```

It is however possible to annotate an operation with the `smithy4s.meta#unpackedOutput` trait, in order for the rendered method to return the type of the output structure's single member directly, without the wrapping case class.

For instance:

```kotlin
use smithy4s.meta#unpackedOutput

@unpackedOutput
operation GetItem {
  output: GetItemOutput
}
```

will produce the following Scala code:

```scala
trait ItemServiceGen[F[_]] {

  def getItem(): F[Item]

}
```

The trait can only be applied to operations whose output is a structure containing **exactly one member**. This is enforced by a Smithy validator; applying `@unpackedOutput` to an operation with a zero-member or multi-member output will fail model validation.

The wire format is unchanged by this trait: HTTP bindings (`@httpPayload`, `@httpHeader`, `@httpResponseCode`, etc.) applied to the single member continue to behave as if the wrapping structure were present. `@unpackedOutput` only affects the shape of the generated Scala method.
