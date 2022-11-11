---
sidebar_label: Client
title: SimpleRestJson client
---

The `smithy4s-http4s` module provides functions that transform low-level http4s clients into high-level stubs, provided the corresponding service definitions (in smithy) are annotated with the `alloy#simpleRestJson` protocol.
- Uri is optional as it will default to http://localhost:8080
In `build.sbt`
- the max arity of json array decoder is 1024 by default, but can be changed by calling withMaxArity on the RestJsonBuilder object

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
  def helloWorldClient(http4sClient: Client[IO]) : Resource[IO, HelloWorldService[IO]] =
    HelloWorldService.simpleRestJson
    .client(http4sClient)
    .uri(Uri.unsafeFromString("http://localhost"))
    .resource

  // alternatively ... with setting the max arity to 2048
  def helloWorldClient2(http4sClient: Client[IO]) : Resource[IO, HelloWorldService[IO]] =
    SimpleRestJsonBuilder
    .withMaxArity(2048)(HelloWorldService)
    .client(http4sClient)
      .uri(Uri.unsafeFromString("http://localhost"))
      .resource
    
}
```
