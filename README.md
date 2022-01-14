[![CI](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml/badge.svg)](https://github.com/disneystreaming/smithy4s/actions/workflows/ci.yml)
[![smithy4s-core Scala version support](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core/latest-by-scala-version.svg)](https://index.scala-lang.org/disneystreaming/smithy4s/smithy4s-core)
[![CLA assistant](https://cla-assistant.io/readme/badge/disneystreaming/smithy4s)](https://cla-assistant.io/disneystreaming/smithy4s)

# smithy4s

[Smithy](https://awslabs.github.io/smithy/) is an interface definition language (IDL) provided by AWS. It is protocol agnostic, flexible, and reasonably low surface, which facilitates the writing of tooling.

smithy4s is a tool that generates third-party-free, protocol-agnostic scala code from smithy specifications, and provides opt-in modules containing functions that use third-party libraries to interpret the generated code in different ways.

Smithy4s can be used to quickly derive http/rest servers and clients, but also pure-scala AWS clients.

## Usage

### SBT plugin

`smithy4s-sbt-codegen` is a code generator plugin that creates `.scala` models and stubs out of the `.smithy` specs. The generated code does not depend on any third-party dependency for compilation, whether http-related, json-related or otherwise.

In `project/plugins.sbt` :

```scala
addSbtPlugin("com.disneystreaming.smithy4s"  % "smithy4s-sbt-codegen" % "x.y.z")
```

and enable the plugin in the desired sbt module :

```scala
import smithy4s.codegen.Smithy4sCodegenPlugin

val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  // version for smithy4s-core is sourced from Smithy4sCodegenPlugin
  .settings(libraryDependencies += "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value)
```

This will enable the plugin on `myModule`. We also need to add `smithy4s-core here since it is needed
for compiling the generated code.

This will look for the smithy specs in the folder `$MY_MODULE/src/main/smithy` and will write scala code in `$MY_MODULE/target/scala-<version>/src_managed/` when invoking `compile`. The paths are configurable via the `smithy4sInputDir` and `smithy4sOutputDir` settings keys.

For example, in order for the plugin to source `.smithy` specs from `./smithy_input` (inside the folder where our `build.sbt` is) and output the generated files into `./smithy_output`.

```scala
val myModule = project
  .in(file("modules/my-module"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    scalaVersion := "2.13.6",
    smithy4sInputDir in Compile  := (baseDirectory in ThisBuild).value / "smithy_input",
    smithy4sOutputDir in Compile := (baseDirectory in ThisBuild).value / "smithy_output",
    libraryDependencies += "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
  )

```

### Command-line

Beside the baked-in SBT plugin, smithy4s comes with a CLI, that allows to generate Scala code and OpenAPI specs from smithy specs.

We recommend using [coursier](https://get-coursier.io/docs/cli-launch) to install/run it

#### Installation

cs install --channel https://disneystreaming.github.io/coursier.json smithy4s

#### Usage

```bash 
bash> smithy4s generate ./foo.smithy ./bar.smithy
```

The CLI comes with a number of options to customise output directories, skip openapi generation (or scala generation), provide a filter of allowed namespaces, etc. Use the `--help` command to get an exhaustive listing.

## Http services (REST/json)

Smithy4s contains generic interpreters that provide http routing logic, given an implementation of a generated interfaces. It is a good way to get http services started quickly, as you can focus on the implementation of business logic whilst leaving the error-prone http and serialisation logic to the care of the library.

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

* Resources (CRUD specialised services)

### The simpleRestJson protocol

This library provides a custom protocol that rest services *should* be annotated with (it'll eventually become mandatory in smithy4s).

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



### REST-json clients/servers

#### http4s
##### Server

smithy4s provides functions that allow to transform high-level service implementations into low level http routes.

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

##### Client

Smithy4s provides functions to transform low-level http4s clients into a high-level smithy service client.

In `Clients.scala`

```scala mdoc:compile-only
import smithy4s.http4s._
import org.http4s.Uri
import org.http4s.client.Client

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

#### Swagger-ui

smithy4s will automatically generate an openapi "view" of all service specifications that are annotated with a protocol trait that supports openapi
conversion. We provide one out of the box, called `simpleRestJson`.

```kotlin
namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
}
```


In addition, the http4s-swagger module provides a one liner function to serve swagger-ui using that view. By default, the documentation is routed under the `/docs` path.

In `build.sbt`

```scala
libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-http4s-swagger" % smithy4sVersion.value
)
```

In `Docs.scala`

```scala mdoc:compile-only
import org.http4s._
import cats.effect.IO

object Docs {
  val myDocRoutes : HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)
}
```

As a reminder, http4s' `HttpRoutes` can be composed using the `<+>` operator

```scala mdoc:compile-only
import smithy4s.http4s._
import smithy4s.http4s.swagger.docs
import cats.implicits._
import org.http4s.implicits._

// ...
val docRoutes = docs[IO](HelloWorldService)
val app = SimpleRestJsonBuilder
  .routes(HelloWorldImpl)
  .make
  .map(serviceRoutes => docRoutes <+> serviceRoutes)
  .map(_.orNotFound)
// ...
```

### AWS

**WARNING: THIS IS EXPERIMENTAL, DO NOT NOT EXPECT PRODUCTION READINESS**

smithy4s provides functions to create AWS clients from generated code. At the time of writing this, smithy4s is only able to derive clients for AWS services that use the [AWS Json 1.0/1.1 protocol](https://awslabs.github.io/smithy/1.0/spec/aws/index.html?highlight=aws%20protocols#aws-protocols).

The AWS smithy specs (that are written in json syntax) can be found in some of the [official SDKs](https://github.com/aws/aws-sdk-js-v3/tree/main/codegen/sdk-codegen/aws-models) published by AWS. These `.json files` can be understood by smithy4s, just like `.smithy`, and can be used to generate code. Just copy/paste them in your project.

We (the smithy4s maintainers) **do not** intend to publish pre-generated artifacts containing the AWS clients, there's a lot of nuance there and maintainance burden that we do not have the capacity to assume. In particular, backward binary compatibility of the generated code is impossible to guarantee at this time.

#### Setup

In `build.sbt`

```scala
libraryDependencies ++= Seq(
  // version sourced from the plugin
  "com.disneystreaming.smithy4s"  %% "smithy4s-aws-http4s" % smithy4sVersion.value
)
```

In your scala code :

```scala mdoc:compile-only
import cats.effect._
import org.http4s.ember.client.EmberClientBuilder

import smithy4s.aws._ // AWS models and cats-effect/fs2 specific functions
import smithy4s.aws.http4s._ // AWS/http4s specific integration
import com.amazonaws.dynamodb._ // Generated code from specs.

object Main extends IOApp.Simple {

  def run = resource.use { dynamodb =>
    dynamodb
      .describeTable(TableName("omelois-test"))
      .run
      .flatMap(IO.println(_))
  }

  val resource: Resource[IO, AwsClient[DynamoDBGen, IO]] = for {
    httpClient <- EmberClientBuilder.default[IO].build
    dynamodb <- DynamoDB.awsClient(httpClient, AwsRegion.US_EAST_1)
  } yield dynamodb
}

```

## Benchmarks

There's a module with benchmarks to compare against the handcrafted implementations of an http4s / Play with the generic ones run the benchmarks, one should execute:

```sh
sbt benchmark/jmh:run
```

To run http4s benchmarks:

```sh
benchmark/jmh:run .*Http4sBenchmark.*
```

To benchmark with Scala 2.12 version instead of 2.13, use `benchmark2_12/jmh:run` instead.
