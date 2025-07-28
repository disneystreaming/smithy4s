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
import smithy4s.example.hello._

object Clients {
  def helloWorldClient2(http4sClient: Client[IO]): Resource[IO, HelloWorldService[IO]] =
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

If the `X-Error-Type` header is provided, smithy4s clients will attempt to use the status code to decide which error type to utilize. They do so as follows:

1. First, we look for an exact match. The response code is compared against the value of the `@httpError` trait on the operation's error types. If there's more than one match, a `RawErrorResponse` is raised. If there were no exact matches, we move on to step 2.
2. Now, we check the category of the response code: if it's a `4xx` (e.g. 404 or 401), we look for **exactly one** error marked with `@error("client")`. If it's a `5xx`, we look for `@error("server")` instead. Again, in case of more than one match, we raise a `RawErrorResponse`.

#### Total failure of decoding

If all the above methods fail to find a suitable error type to decode the response as, OR if one is found but the response doesn't match its Smithy schema, a `RawErrorResponse` is raised.

The `RawErrorResponse` class carries information about the response that produced it. It also holds information about _why_ it was raised (in a `FailedDecodeAttempt`), e.g.

- the decoding couldn't figure out which error type to use (`FailedDecodeAttempt.UnrecognisedDiscriminator`)
- an error type was found but it failed to decode (`FailedDecodeAttempt.DecodingFailure`).

#### Examples

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

| Status Code | Error Selected          |
| ----------- | ----------------------- |
| 404         | NotFoundError           |
| 400         | **RawErrorResponse**    |
| 503         | ServiceUnavailableError |
| 500         | CatchAllServerError     |

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

| Status Code | Error Selected          |
| ----------- | ----------------------- |
| 404         | **RawErrorResponse**    |
| 400         | RawErrorResponse        |
| 503         | ServiceUnavailableError |
| 500         | CatchAllServerError     |

Now the 404 status code cannot be mapped. This is due to the fact that two different error types are annotated with a 404 `httpError` trait. This means that the smithy4s
client is not able to decide which of these errors is correct.
