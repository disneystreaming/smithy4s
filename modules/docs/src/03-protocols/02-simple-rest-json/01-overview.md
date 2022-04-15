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

structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
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

## Decoding and encoding unions

There is no one way to encode a union in JSON. You can use tagged union, untagged union or discriminated union. Smithy4s supports all three of them. The following example assume the following shapes are available:

```
structure One {
  one: String
}

structure Two {
  two: Int
}
```

### Tagged union

This is the default behaviour, and it matches Smithy's encoding of union. The rational is simple: there is a key indicating what shape we should decode/encode to. Given the following union:

```
union Tagged {
  first: One,
  second: Two
}
```

Smithy4s will render encode / decode an array of `Tagged` this way:

```json
[
  { "first": { "one": "smithy4s" } },
  { "second": { "two": 42 } },
]
```

### Untagged union

Untagged union are supported via an annotation: `@untagged`. Here, there is no way for the encoder/decoder to know what it is working with. It will do a best effort to figure out what shape it is working with given the content. Given the following union:

```
use smithy4s.api#untagged

@untagged
union Untagged {
  first: One,
  second: Two
}
```

Smithy4s will render encode / decode an array of `Untagged` this way:

```json
[
  { "one": "smithy4s" },
  { "two": 42 },
]
```

### Discriminated union

Discriminated union are supported via an annotation: `@discriminated("tpe")`. Here, the encoder/decoder looks for the field specified in the annotation to know what shape it is working with. Given the following union:

```
use smithy4s.api#discriminated

@discriminated("tpe")
union Untagged {
  first: One,
  second: Two
}
```

Smithy4s will render encode / decode an array of `Untagged` this way:

```json
[
  { "tpe": "first",  "one": "smithy4s" },
  { "tpe": "seconf", "two": 42 },
]
```