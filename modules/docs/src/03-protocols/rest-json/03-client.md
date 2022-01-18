---
sidebar_label: Client
---

# REST JSON Client

Smithy4s provides functions to transform low-level http4s clients into a high-level smithy service client.

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
  def helloWorldClient(http4sClient: Client[IO]) : Resource[IO, HelloWorldService[IO]] =
    HelloWorldService.simpleRestJson.clientResource(
      http4sClient,
      Uri.unsafeFromString("http://localhost")
    )

  // alternatively ...
  def helloWorldClient2(http4sClient: Client[IO]) : Resource[IO, HelloWorldService[IO]] =
    SimpleRestJsonBuilder(HelloWorldService).clientResource(
      http4sClient,
      Uri.unsafeFromString("http://localhost")
    )
}
```
