---
sidebar_label: SimpleRestJson
title: The SimpleRestJson protocol
---

Smithy4s provides a custom Json-in/Json-out protocol that smithy services can be annotated with.

Smithy4s comes with opt-in http4s-specific module, that contains functions that are aware of this protocol, and can be used to quickly derive http services and clients.

As for the json aspect of the protocol, [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala/) is used for the (de)serialisation of the http bodies.

## Semantics

In this protocol, the values in shapes are bound to http metadata or body according to the specification of the [Http Binding traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html?highlight=http#http-binding-traits). However, the `@mediaType` trait has no incidence, and all bodies (when present) are serialised in JSON.

## Example spec

```kotlin
namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0"
  // Indicates that all operations in `HelloWorldService`,
  // here limited to the Hello operation, can return `GenericServerError`.
  errors: [GenericServerError]
  operations: [Hello]
}

@error("server")
@httpError(500)
structure GenericServerError {
  message: String
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
  input: Person
  output: Greeting
}

structure Person {
  @httpLabel
  @required
  name: String

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}
```

## Supported traits

This protocol and its interpreters, are aware of the following traits provided out of the box:

* [all simple shapes](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes)
* composite data shapes, including collections, unions, structures.
* [operations and services](https://awslabs.github.io/smithy/1.0/spec/core/model.html#service)
* [enumerations](https://awslabs.github.io/smithy/1.0/spec/core/constraint-traits.html#enum-trait)
* [error trait](https://awslabs.github.io/smithy/1.0/spec/core/type-refinement-traits.html#error-trait)
* [http traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html), including **http**, **httpError**, **httpLabel**, **httpHeader**, **httpPayload**, **httpQuery**, **httpPrefixHeaders**, **httpQueryParams**.
* [timestampFormat trait](https://awslabs.github.io/smithy/1.0/spec/core/protocol-traits.html?highlight=timestampformat#timestampformat-trait)

## Decoding and encoding unions

The `SimpleRestJson` protocol supports 3 different union encodings :

* tagged (default)
* untagged
* discriminated

See the section about [unions](../../04-codegen/02-unions.md) for a detailed description.

## Supported traits

Here is the list of traits supported by `SimpleRestJson`

```scala mdoc:passthrough
println(
smithy4s.api.SimpleRestJson.hints
  .get[smithy.api.ProtocolDefinition]
  .getOrElse(sys.error("Unable to grab protocol defition information."))
  .traits.toList.flatten.map(_.value)
  .map(id => s"- `$id`")
  .foreach(println)
)
```

Currently, `@cors` is not supported. This is because the `@cors` annotation is too restrictive. You can still use it in your model and configure your API using the information found in the generated code. See the [`Cors.scala`](https://github.com/disneystreaming/smithy4s/tree/main/modules/guides/src/smithy4s/guides/Cors.scala) file in the `guides` module for an example.