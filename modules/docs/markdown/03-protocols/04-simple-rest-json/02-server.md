---
sidebar_label: Server
title: SimpleRestJson server
---

The `smithy4s-http4s` module provides functions that transform instances of the generated interfaces into http4s routes, provided the corresponding service definitions (in smithy) are  annotated with the `alloy#simpleRestJson` protocol.

In `build.sbt`

```scala
libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-http4s" % smithy4sVersion.value
)
```

In `MyHelloWorld.scala`, implement the service interface that is generated at build-time:

```scala mdoc:silent
// the package under which the scala code was generated
import smithy4s.example.hello._

import cats.effect.IO

object HelloWorldImpl extends HelloWorldService[IO] {

  def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    town match {
      case None => Greeting(s"Hello $name !")
      case Some(t) => Greeting(s"Hello $name from $t !")
    }
  }

}
```

In `Routes.scala`

```scala mdoc:silent
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s._
import cats.effect.IO
import cats.effect.Resource

object Routes {
  // This can be easily mounted onto a server.
  val myRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource
}
```

## A note about errors

When encountering data types annotated with the `@error` trait in smithy, smithy4s will ensure that the generated types extend `Throwable`. The interpreters are aware of it, and try to recover any error raised in your effect-types that your smithy specs know about, in order to render it correctly in Json and apply the specified status code (see the `@httpError` trait for this).

As a convenience, Smithy4s provides `mapErrors` and `flatMapErrors` methods, that allow to intercept exceptions that were not specified in the spec, and transform them into exceptions that were.

In particular, the smithy4s interpreters raise specific errors when they fail to decode http requests. The `mapErrors` and `flatMapErrors` methods can be used to ensure that a specified error is returned by your service:

```scala
myRoutes.mapErrors{
  case e: PayloadError => MyClientError(...)
}.resource
```

## Wiring the routes

As a reminder, to wire those routes into a server, you need something like:

```scala mdoc:compile-only
import cats.effect._
import org.http4s.ember.server._
import org.http4s.implicits._
import com.comcast.ip4s._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Routes.myRoutes.flatMap { routes =>
      EmberServerBuilder.default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }.use(_ => IO.never)
     .as(ExitCode.Success)
}
```
