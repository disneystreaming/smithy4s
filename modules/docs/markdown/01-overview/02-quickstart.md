---
sidebar_label: Quick Start
title: Quick Start
---

Below is a quick example of smithy4s in action. This page does not provide much explanation or detail. For more information on various aspects of smithy4s, read through the other sections of this documentation site.

## For sbt

This section will get you started with a simple `sbt` module that enables smithy4s code generation. For a similar setup for mill, see [Mill](#for-mill) below.

### project/plugins.sbt

Add the `smithy4s-sbt-codegen` plugin to your build.

```scala
addSbtPlugin("com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "@VERSION@")
```

### build.sbt

Enable the plugin in your project, add the smithy and http4s dependencies.

```scala
import smithy4s.codegen.Smithy4sCodegenPlugin

ThisBuild / scalaVersion := "@SCALA_VERSION@"

val example = project
  .in(file("modules/example"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "@HTTP4S_VERSION@"
    )
  )
```

## For Mill

This section will get you started with a `mill` module with code generation enabled on it.

In your `build.sc`:

```scala
import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::@VERSION@`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object example extends ScalaModule with Smithy4sModule {
  def scalaVersion = "2.13.8"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}",
    ivy"com.disneystreaming.smithy4s::smithy4s-http4s-swagger:${smithy4sVersion()}",
    ivy"org.http4s::http4s-ember-server:@HTTP4S_VERSION@"
  )
}
```

## Smithy content

Now is the time to add some Smithy shapes to see what code generation can do for you. Following the setup above, the location for the Smithy content will change depending on what build tool you used.

Now let's define an API in Smithy. Create the following file:

- for `sbt`, you'll write in `modules/example/src/main/smithy/ExampleService.smithy`.
- for `mill`, you'll write in `example/smithy/ExampleService.smithy`

And add the content below:

```kotlin
namespace smithy4s.hello

use alloy#simpleRestJson

@simpleRestJson
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

The Scala code corresponding to this smithy file will be generated the next time you compile your project.

## Using the generated code

Now, let's use the generated code by the service. You need to create a scala file at the following location:

- for sbt `modules/example/src/main/scala/Main.scala`
- for mill `example/src/Main.scala`

Implement your service by extending the generated Service trait. Wire up routes into server.

```scala mdoc:silent
import smithy4s.example.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder

object HelloWorldImpl extends HelloWorldService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    town match {
      case None => Greeting(s"Hello $name!")
      case Some(t) => Greeting(s"Hello $name from $t!")
    }
  }
}

object Routes {
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

  val run = Routes.all
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }
    .use(_ => IO.never)

}
```

## Run Service

- for sbt: `sbt "example/run"`
- for mill: `mill example.run`

## Navigate to localhost:9000/docs

Here you will find the automatically generated SwaggerUI which will allow you to easily test your API.

![SwaggerUI documentation site request](https://i.imgur.com/WQgetF6.png)

![SwaggerUI documentation site response](https://i.imgur.com/JRUQyny.png)

## Client Example

You can also generate a client using smithy4s.

```scala mdoc:compile-only
import org.http4s.ember.client.EmberClientBuilder

object ClientImpl extends IOApp.Simple {

  val helloWorldClient: Resource[IO, HelloWorldService[IO]] = for {
    client <- EmberClientBuilder.default[IO].build
    helloClient <- SimpleRestJsonBuilder(HelloWorldService)
    .client(client)
    .uri(Uri.unsafeFromString("http://localhost:9000"))
    .resource
  } yield helloClient

  val run = helloWorldClient.use(c =>
    c.hello("Sam", Some("New York City"))
      .flatMap(greeting => IO.println(greeting.message))
  )

}
```
