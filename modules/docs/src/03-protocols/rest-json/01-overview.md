# Overview

Smithy4s provides a custom protocol that REST services *should* be annotated with (it'll eventually become mandatory in smithy4s).

The annotation is required for generation open-api "views" of smithy specs.

```kotlin
namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  // Indicates that all operations in `HelloWorldService`,
  // here limited to Hello, can return `GenericServerError`.
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
  input: Person,
  output: Greeting
}
```

Smithy4s provides `mapErrors` and `flatMapErrors` methods, that allows to leverage this service-wide errors by taking a `Throwable => Throwable` transformation. This same mechanism can also be used to override the out-of-the-box "client" errors :

```scala
routes(...).mapErrors{
  case e : PayloadError => MyClientError(...)
}.make
```
