---
sidebar_label: Client
title: SimpleRestJson client
---

The `smithy4s-http4s` module provides functions that transform low-level http4s clients into high-level stubs, provided the corresponding service definitions (in smithy) are annotated with the `alloy#simpleRestJson` protocol.
- Uri is optional as it will default to http://localhost:8080
- the max arity of json array decoder is 1024 by default, but can be changed by calling withMaxArity on the RestJsonBuilder object

In `build.sbt`

```scala
libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-http4s" % smithy4sVersion.value
)
```

In `Clients.scala`

```scala mdoc:compile-only
import smithy4s.http4s._
import org.http4s.Uri
import org.http4s.client.Client
import cats.effect.IO
import cats.effect.Resource

// the package under which the scala code was generated
import smithy4s.hello._

object Clients {
  def helloWorldClient2(http4sClient: Client[IO]) : Resource[IO, HelloWorldService[IO]] =
    SimpleRestJsonBuilder
      .withMaxArity(2048) // prepare maximum array/object size accepted during json decoding
      .apply(HelloWorldService)
      .client(http4sClient)
      .uri(Uri.unsafeFromString("http://localhost"))
      .resource

}
```

## Error Handling

Smithy4s clients map HTTP error responses to the errors defined in the underlying smithy model in the following ways:

#### X-Error-Type

If an `X-Error-Type` header is present on the response from the server, the value of this header is used to map the response to a specific error type.
The header's value can be either the ShapeId of the error it is targeting OR the name without the namespace. The value of this header is case sensitive.
Here are some examples of an X-Error-Type header:

`X-Error-Type: NotFoundError` AND `X-Error-Type: example.test#NotFoundError` could each be used to map to the error type `NotFoundError`. This of course assumes
that the `NotFoundError` is provided in either the service or operation `errors` array provided in the smithy model.

All smithy4s services provide an `X-Error-Type` in responses by default.

#### Status Code

If the `X-Error-Type` header is not defined, smithy4s clients will use the status code to attempt to decide which error type to utilize. It does so as follows:

1. If there is a single Error type that contains the correct status code in the `httpError` trait, this type will be used. If there are two error types with the same status code, an `UnknownErrorResponse` will be surfaced to the client.
2. If there is NOT a matching status code, but there is a single error marked with the `error` trait, this error type will be used as long as the returned status code is in the range for either a client or server error. In other words if a single error shape has no status code, but is annotated with `@error("client")` and the returned status code is 404 then this error type will be used. If there are multiple error types with no status code and a matching error type (client/server), then an `UnknownErrorResponse` will be surfaced to the client.

Here are some examples to show more what this looks like.

Example smithy model:

```kotlin
operation TestOp {
  ...
  errors: [NotFoundError, ServiceUnavailableError, CatchAllClientError, CatchAllServerError]
}

@httpError(404)
@error("client")
structure NotFoundError {
  message: String
}

@httpError(503)
@error("server")
structure ServiceUnavailableError {
  message: String
}

@error("client")
structure CatchAllClientError {
  message String
}

@error("server")
structure CatchAllServerError {
  message String
}
```

And here are some scenarios using this example model. For all of these, assume that NO `X-Error-Type` header is provided.

| Status Code | Error Selected          |
| ----------- | ----------------------- |
| 404         | NotFoundError           |
| 400         | CatchAllClientError     |
| 503         | ServiceUnavailableError |
| 500         | CatchAllServerError     |

However, adding another error to the operation that looks like:

```kotlin
@error("client")
structure AnotherError {
  message: String
}
```

Would result in the following:

| Status Code | Error Selected           |
| ----------- | ------------------------ |
| 404         | NotFoundError            |
| 400         | **UnknownErrorResponse** |
| 503         | ServiceUnavailableError  |
| 500         | CatchAllServerError      |

Notice that the 400 status code cannot be properly mapped. This is because there is no exact match AND there are two errors that are labeled with `@error("client")` which also do not have an associated `httpError` trait containing a status code.

Adding another error type to the operation that looks like:

```kotlin
@httpError(404)
@error("client")
structure AnotherNotFoundError {
  message: String
}
```

Will result in the following:

| Status Code | Error Selected           |
| ----------- | ------------------------ |
| 404         | **UnknownErrorResponse** |
| 400         | UnknownErrorResponse     |
| 503         | ServiceUnavailableError  |
| 500         | CatchAllServerError      |

Now the 404 status code cannot be mapped. This is due to the fact that two different error types are annotated with a 404 `httpError` trait. This means that the smithy4s
client is not able to decide which of these errors is correct.
