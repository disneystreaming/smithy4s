---
sidebar_label: API Definition
---

# Defining your APIs in Smithy

Smithy4s contains generic interpreters that provide http routing logic, given an implementation of a generated interfaces. It is a good way to get http services started quickly, as you can focus on the implementation of business logic whilst leaving the error-prone http and serialization logic to the care of the library.

These interpreters work by looking at the [http-specific traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html) present in your smithy specs.


### Example spec

```kotlin
namespace smithy4s.example

service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
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


### Currently supported

* [all simple shapes](https://awslabs.github.io/smithy/1.0/spec/core/model.html#simple-shapes)
* composite data shapes, including collections, unions, structures.
* [operations and services](https://awslabs.github.io/smithy/1.0/spec/core/model.html#service)
* [enumerations](https://awslabs.github.io/smithy/1.0/spec/core/constraint-traits.html#enum-trait)
* [error trait](https://awslabs.github.io/smithy/1.0/spec/core/type-refinement-traits.html#error-trait)
* [http traits](https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html), including **http**, **httpError**, **httpLabel**, **httpHeader**, **httpPayload**, **httpQuery**, **httpPrefixHeaders**, **httpQueryParams**.
* [timestampFormat trait](https://awslabs.github.io/smithy/1.0/spec/core/protocol-traits.html?highlight=timestampformat#timestampformat-trait)

### Currently **not** supported (in particular)

* Resources (CRUD specialized services)

### The simpleRestJson protocol

This library provides a custom protocol that rest services *should* be annotated with (it'll eventually become mandatory in smithy4s).

The annotation is required for generation open-api "views" of smithy specs.

See [here](protocols/rest-json/overview) for more.
