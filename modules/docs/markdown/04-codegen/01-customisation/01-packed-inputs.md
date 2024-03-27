---
sidebar_label: Packed inputs
title: Packed inputs
---

By default, Smithy4s generates methods the parameters of which map to the fields of the input structure of the corresponding operation.

For instance :

```smithy
service PackedInputsService {
  version: "1.0.0"
  operations: [PackedInputOperation]
}

operation PackedInputOperation {
  input: PackedInput
}

structure PackedInput {
    @required
    a: String
    @required
    b: String
}
```

leads to something conceptually equivalent to :

```scala
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(a: String, b: String): F[Unit]

}
```

It is however possible to annotate the service (or operation) definition with the `smithy4s.meta#packedInputs` trait, in order for the rendered method to contain a single parameter, typed with actual input case class of the operation.

For instance :

```scala
use smithy4s.meta#packedInputs

@packedInputs
service PackedInputsService {
  version: "1.0.0"
  operations: [PackedInputOperation]
}
```

will produce the following Scala code

```scala
trait PackedInputServiceGen[F[_]] {

  def packedInputOperation(input: PackedInput): F[Unit]

}
```
