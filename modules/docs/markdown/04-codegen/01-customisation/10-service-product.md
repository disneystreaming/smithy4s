---
sidebar_label: Service Product
title: Service Product
---

As of smithy4s version `0.18.x` you can also generate a service interface in
which each method doesn't receive an input. Instead, the output of each method
has the usual return type, which already includes the input as a type parameter.
We call this version a "service product" because it can be seen as the product
of all the operations of the service.

To generate a service product, annotate the service definition with

`@generateServiceProduct`:

```smithy
use smithy4s.meta#generateServiceProduct

@generateServiceProduct
service ExampleService {
  version: "1.0.0"
  operations: [ExampleOperation]
}

operation ExampleOperation {
  input: ExampleInput,
  output: ExampleOutput,
  errors: [ExampleError]
}

structure ExampleInput {
  @required
  a: String
}

structure ExampleOutput {
  @required
  b: String
}

@error("server")
structure ExampleError {
  @required
  c: String
}
```

This will generate the following interface:

```scala
trait ExampleServiceProductGen[F[_, _, _, _, _]] {
  self =>

  def exampleOperation: F[ExampleInput, ExampleServiceOperation.ExampleOperationError, ExampleOutput, Nothing, Nothing]
}
```

and the following implementation of `ServiceProduct`:

```scala
object ExampleServiceProductGen extends ServiceProduct[ExampleServiceProductGen]
```

You will be able to access the service product version of the service like this:

```scala
ExampleServiceGen.serviceProduct
```
