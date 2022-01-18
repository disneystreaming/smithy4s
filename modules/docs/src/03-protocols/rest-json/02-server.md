---
sidebar_label: Server
---

# REST JSON Server

Smithy4s provides functions that allow to transform high-level service implementations into low level http routes.

In `build.sbt`

```scala
libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-http4s" % smithy4sVersion.value
)
```

In `MyHelloWorld.scala`, implement the service interface.

```scala mdoc:silent
// the package under which the scala code was generated
import smithy4s.hello._

import cats.effect.IO

object HelloWorldImpl extends HelloWorldService[IO] {

  def hello(name: String, town: Option[String]) : IO[Greeting] = IO.pure {
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
  val myRoutes : Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource
}
```

To wire those routes into a server, as an example, we would need:

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
