---
sidebar_label: Protocols and smithy4s
title: Protocols and smithy4s
---

Smithy4s is opinionated in what the generated code look like, there are a few things that can be tweaked.

#### Packed inputs

By default, smithy4s generates methods the parameters of which map to the fields of the input structure of the corresponding operation.

For instance :

```kotlin
service PackedInputsService {
  version: "1.0.0",
  operations: [PackedInputOperation]
}

operation PackedInputOperation {
  input: PackedInput,
}

structure PackedInput {
    @required
    a: String,
    @required
    b: String
}
```

leads to something conceptually equivalent to :

```scala
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(a: String, b: String) : F[Unit]

}
```

It is however possible to annotate the service (or operation) definition with the `smithy4s.meta#packedInputs` trait, in order for the rendered method to contain a single parameter, typed with actual input case class of the operation.

For instance :

```scala
use smithy4s.meta#packedInputs

@packedInputs
service PackedInputsService {
  version: "1.0.0",
  operations: [PackedInputOperation]
}
```

will produce the following Scala code

```scala  
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(input: PackedInput) : F[Unit]

}
```
