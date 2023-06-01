---
sidebar_label: Compliance Tests
title: Compliance Tests
---

The Smithy prelude has support for compliance testing for services that use `HTTP` as their protocol. It is built on top of regular traits, you can read more about it [over here](https://awslabs.github.io/smithy/2.0/additional-specs/http-protocol-compliance-tests.html).

Basically, you annotate operations and/or structures (that depends on the type of test being defined) and protocol implementors can generate tests cases to ensure their implementation behaves correctly.

Smithy4s publishes a module that you can use to write compliance test if you're implementing a protocol. Add the following to your dependencies if you use `sbt`:

```scala

import smithy4s.codegen.BuildInfo._

libraryDependencies ++= Seq(
  "com.disneystreaming.smithy4s" %% "smithy4s-compliance-tests" % smithy4sVersion.value
)
```

If you use `mill`:

```scala
import smithy4s.codegen.BuildInfo._

def smithy4sIvyDeps = Agg(
  ivy"software.amazon.smithy:smithy-protocol-test-traits:$smithyVersion"
)

def ivyDeps = Agg(
  ivy"com.disneystreaming.smithy4s::smithy4s-compliance-tests:${smithy4sVersion()}"
)
```

The rest of this document will demonstrate how to write a Smithy specification that specify HTTP compliance tests, and then how to use the module mentioned above to run the tests.

_Note: currently, only `httprequesttests-trait` are supported. other traits support will be integrated soon._

## Example specification

```scala mdoc:passthrough
docs.InlineSmithyFile.fromSample("test.smithy")
```

We have a very simple specification: one operation with basic input and output shapes. We've added a `httpRequestTests` to define a compliance test for protocol implementors.

## Testing the protocol

The service in the specification is annotated with the `alloy#simpleRestJson` protocol definition. We'll use the `compliance-tests` module to make sure this protocol can handle such an operation.

_Note: the following code and the `compliance-tests` module do not depend on a specific test framework. If you want to hook it into your test framework, it is easy to do so but it's outside the scope of this document. Refer to [this example](@GITHUB_BRANCH_URL@modules/compliance-tests/test/src/smithy4s/compliancetests/WeaverComplianceTest.scala) to see how we did it for `Weaver` in this project._

First, some imports:

```scala mdoc:silent
import cats.effect._
import org.http4s._
import org.http4s.client.Client
import smithy4s.compliancetests._
import smithy4s.example.test._
import smithy4s.http.HttpMediaType
import smithy4s.http4s._
import smithy4s.kinds._
import smithy4s.Service
import smithy4s.schema.Schema
```

Then, you can create and instance of `ClientHttpComplianceTestCase` and/or `ServerHttpComplianceTestCase` while selecting the protocol to use and the service to test:

```scala mdoc:silent
object SimpleRestJsonIntegration extends Router[IO] with ReverseRouter[IO] {
    type Protocol = alloy.SimpleRestJson
    val protocolTag = alloy.SimpleRestJson

    def expectedResponseType(schema: Schema[_]) = HttpMediaType("application/json")

    def routes[Alg[_[_, _, _, _, _]]](
        impl: FunctorAlgebra[Alg, IO]
    )(implicit service: Service[Alg]): Resource[IO, HttpRoutes[IO]] =
      SimpleRestJsonBuilder(service).routes(impl).resource

    def reverseRoutes[Alg[_[_, _, _, _, _]]](app: HttpApp[IO],testHost: Option[String] = None)(implicit
        service: Service[Alg]
    ): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      import org.http4s.implicits._
      val baseUri = uri"http://localhost/"
      val suppliedHost = testHost.map(host => Uri.unsafeFromString(s"http://$host"))
      SimpleRestJsonBuilder(service)
        .client(Client.fromHttpApp(app))
        .uri(suppliedHost.getOrElse(baseUri))
        .resource
    }
  }

val tests: List[ComplianceTest[IO]] = HttpProtocolCompliance
    .clientAndServerTests(SimpleRestJsonIntegration, HelloWorldService)
```

Now, you can iterate over the test cases and do what you want. This is where you would hook in the test framework of your choice, but in the following example, we're just going to print the result:

```scala mdoc:silent
import cats.syntax.traverse._
import cats.effect.unsafe.implicits.global

val runTests: IO[List[String]] = tests
  .map { tc =>
    tc.run.map(_.toEither).map {
      case Left(value) =>
        s"Failed ${tc.show} with the following message: $value"
      case Right(_) => s"Success ${tc.show}"

    }
  }
  .sequence
```

Will produce the following when executed:

```scala mdoc:passthrough
println("```")
println(runTests.map(_.mkString("\n")).unsafeRunSync())
println("```")
```
