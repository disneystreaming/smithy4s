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
  version: "1.0.0",
  // Indicates that all operations in `HelloWorldService`,
  // here limited to the Hello operation, can return `GenericServerError`.
  errors: [GenericServerError],
  operations: [Hello]
}

@error("server")
@httpError(500)
structure GenericServerError {
  message: String
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
  input: Person,
  output: Greeting
}
```

## Supported taits

This protocol and its interpreters, are aware of the following traits provided out of the box:

* [all simple shapes](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes)
* composite data shapes, including collections, unions, structures.
* [operations and services](https://awslabs.github.io/smithy/1.0/spec/core/model.html#service)
* [enumerations](https://awslabs.github.io/smithy/1.0/spec/core/constraint-traits.html#enum-trait)
* [error trait](https://awslabs.github.io/smithy/1.0/spec/core/type-refinement-traits.html#error-trait)
* [http traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html), including **http**, **httpError**, **httpLabel**, **httpHeader**, **httpPayload**, **httpQuery**, **httpPrefixHeaders**, **httpQueryParams**.
* [timestampFormat trait](https://awslabs.github.io/smithy/1.0/spec/core/protocol-traits.html?highlight=timestampformat#timestampformat-trait)
