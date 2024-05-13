---
sidebar_label: Middlewares
title: Middlewares
---

It is not the purpose of Smithy4s to reach feature-parity with the official Java SDK, in particular when it comes to retry policies and metrics. However, it's important for Smithy4s to empower users to wire custom behaviour in the AWS clients it provides.

To that end, Smithy4s provides a middleware mechanism :

```scala mdoc:compile-only
import smithy4s._
import smithy4s.aws._
import cats.effect._
import cats.syntax.all._
import org.http4s.client._

def http4sClient : Client[IO] = ???

object PrintingMiddleware extends Endpoint.Middleware.Standard[Client[IO]] {
  def prepare(serviceId: ShapeId, endpointId: ShapeId, serviceHints: Hints, endpointHints: Hints): Client[IO] => Client[IO] =
      underlyingClient => Client { req =>
        IO.println(s"Calling ${serviceId.name}.${endpointId.name}").toResource *>
        underlyingClient.run(req)
      }
}

val awsEnvironment = AwsEnvironment.default(http4sClient, AwsRegion.US_EAST_1).map(_.withMiddleware(PrintingMiddleware))
```

The `Endpoint.Middleware` interface allows to provide transformations that will be applied on per-endpoint basis, allowing to customise
the behaviour of the client based on the calls that are made to AWS.
