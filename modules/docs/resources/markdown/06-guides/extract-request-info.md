---
sidebar_label: Extracting Request Info
title: Extracting Request Information
---

There are times where the implementation of your route handlers may require more information from the underlying http4s request.
You may want to store the raw request, check the request body against an HMAC value, pass along header values for tracing spans, etc.

When possible, the implementation of things like this should be isolated to middleware and not passed into the route handlers themselves.
However, there are times where this isn't possible. This guide shows you how to implement a middleware that uses `IOLocal` to pass the value
of several headers from the request into the smithy4s route handlers.

## What is IOLocal?

[IOLocal](https://github.com/typelevel/cats-effect/blob/series/3.x/core/shared/src/main/scala/cats/effect/IOLocal.scala)
is a construct that allows for sharing context across the scope of a `Fiber`. This means it allows you to get and set some value `A` in the `IOLocal`.
This value will be accessible across the current `Fiber`. As a `Fiber` is forked into new fibers, the value of `A` is carried over to the new `Fiber`.
However, the new `Fiber` will not be able to update the value kept on its parent or sibling fibers.

This diagram, adapted from the [IOLocal docs](https://github.com/typelevel/cats-effect/blob/series/3.x/core/shared/src/main/scala/cats/effect/IOLocal.scala), illustrates this well:

![IOLocal flow chart](/img/ioLocalDiagram.svg)

## Example Implementation

### Smithy Spec

For this example, we are going to be working with the following smithy specification (taken from [smithy4s repo](@GITHUB_BRANCH_URL@sampleSpecs/hello.smithy)):

```scala mdoc:passthrough
docs.InlineSmithyFile.fromSample("hello.smithy")
```

See our [getting started documentation](../01-overview/01-intro.md) for instructions on how to use this specification to generate scala code.

### Service Implementation

Let's start by creating a case class that we will use to hold the value of some headers from our request.

```scala mdoc:silent
case class RequestInfo(contentType: String, userAgent: String)
```

This class will give us a spot to place the `Content-Type` and `User-Agent` headers, respectively. These are just shown
as an example. We could instead pass any other header or part of the request.

From here, we can implement the `HelloWorldService` interface that smithy4s generated from the specification above.

```scala mdoc:silent
import smithy4s.example.hello._
import cats.effect.IO
import cats.effect.IOLocal

final class HelloWorldServiceImpl(requestInfo: IO[RequestInfo]) extends HelloWorldService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] =
    requestInfo.flatMap { reqInfo: RequestInfo =>
      IO.println("REQUEST_INFO: " + reqInfo)
        .as(Greeting(s"Hello, $name"))
    }
}
```

This is a basic implementation that, in addition to returning a `Greeting`, prints the `RequestInfo` out to the console.
Note that it is getting the `RequestInfo` from the `IO[RequestInfo]` that is being passed in as a constructor parameter. This `IO`
will be created using the same `IOLocal` instance is passed to our middleware implementation.
That way, the middleware can set the `RequestInfo` value that we are reading here.

### Middleware

Below is the middleware implementation. It extracts the `Content-Type` and `User-Agent` headers and passes them along in the `IOLocal`
instance it is provided.

```scala mdoc:silent
import cats.data._
import org.http4s.HttpRoutes
import cats.syntax.all._
import org.http4s.headers.{`Content-Type`, `User-Agent`}

object Middleware {

  def withRequestInfo(
      routes: HttpRoutes[IO],
      local: IOLocal[Option[RequestInfo]]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request =>
      val requestInfo = for {
        contentType <- request.headers.get[`Content-Type`].map(ct => s"${ct.mediaType.mainType}/${ct.mediaType.subType}")
        userAgent <- request.headers.get[`User-Agent`].map(_.product.toString)
      } yield RequestInfo(
        contentType,
        userAgent
      )
      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

}
```

### Wiring it Together

Now that we have our service implementation and our middleware, we need to combine them to create our application.

```scala mdoc:silent
import cats.effect.kernel.Resource

object Routes {
  private val docs =
    smithy4s.http4s.swagger.docs[IO](smithy4s.example.hello.HelloWorldService)
  def getAll(local: IOLocal[Option[RequestInfo]]): Resource[IO, HttpRoutes[IO]] = {
    val getRequestInfo: IO[RequestInfo] = local.get.flatMap {
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
    }
    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new HelloWorldServiceImpl(getRequestInfo))
      .resource
      .map { routes =>
        Middleware.withRequestInfo(routes <+> docs, local)
      }
  }
}
```

Here we are creating our routes (with swagger docs) and passing them to our middleware. The result of applying the Middleware
is our final routes.

We also turn our `IOLocal` into an `IO[RequestInfo]` for the `HelloWorldServiceImpl`. We do this because the service implementation
does not need to know that the value is coming from an `IOLocal` or that the value is optional (since it will always be populated by
our middleware). Doing it this way allows us to reduce the complexity in the service implementation.

Finally, we create our main class and construct the http4s server.

```scala mdoc:silent
import cats.effect.IOApp
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder

object Main extends IOApp.Simple {
  def run: IO[Unit] = IOLocal(Option.empty[RequestInfo]).flatMap { local =>
    Routes
      .getAll(local)
      .flatMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"localhost")
          .withPort(port"9000")
          .withHttpApp(routes.orNotFound)
          .build
      }
      .useForever
  }
}
```

Notice that we create the `IOLocal` with `Option.empty[RequestInfo]`. This is because `IOLocal` requires a value
to be constructed. However, this value will never be used in practice. This is because we are setting the value in
the middleware on every request prior to the request being handled by our `HelloWorldService` implementation.

### Testing it out

With the above in place, we can run our application and test it out.

```
curl -X 'POST' \
  'http://localhost:9000/Test' \
  -H 'User-Agent: Chrome/103.0.0.0' \
  -H 'Content-Type: application/json'
```

Running this `curl` will cause the following to print out to the console:

```
REQUEST_INFO: RequestInfo(Some(application/json),Some(Chrome/103.0.0.0))
```

## Alternative Methods

If you are working with a tagless `F[_]` rather than `IO` directly, you may want to check out Chris Davenport's [implementation
of FiberLocal](https://github.com/davenverse/fiberlocal/).

You can also use `Kleisli` to accomplish the same things we showed in this tutorial and you are welcome to do so if you prefer that.
We opted to show an example with `IOLocal` since it allows users to use `IO` directly, without monad transformers, which many
users will be more comfortable with. Similarly, you could use `Local` from cats-mtl or probably a variety of other approaches.
We recommend you use whatever fits the best with your current application design.
