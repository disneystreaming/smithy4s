/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s
package http4s

import cats.data.OptionT
import cats.effect.IO
import cats.effect.Resource
import cats.Eq
import cats.implicits._
import fs2.Collector
import org.http4s._
import org.http4s.client.Client
import org.http4s.HttpApp
import org.http4s.Uri
import smithy4s.hello._
import weaver._

object ServerEndpointMiddlewareSpec extends SimpleIOSuite {

  private implicit val greetingEq: Eq[Greeting] = Eq.fromUniversalEquals
  private implicit val throwableEq: Eq[Throwable] = Eq.fromUniversalEquals

  final class MiddlewareException
      extends RuntimeException(
        "Expected to recover via flatmapError or mapError"
      )
  test("server - middleware can throw and mapped / flatmapped") {
    val middleware = new ServerEndpointMiddleware.Simple[IO]() {
      def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[IO] => HttpApp[IO] = { inputApp =>
        HttpApp[IO] { _ => IO.raiseError(new MiddlewareException) }
      }
    }
    def runOnService(service: HttpRoutes[IO]): IO[Expectations] =
      service(Request[IO](Method.POST, Uri.unsafeFromString("/bob")))
        .flatMap(res => OptionT.pure(expect.eql(res.status.code, 599)))
        .getOrElse(
          failure("unable to run request")
        )

    val pureCheck = runOnService(
      SimpleRestJsonBuilder
        .routes(HelloImpl)
        .middleware(middleware)
        .flatMapErrors { case _: MiddlewareException =>
          IO.pure(SpecificServerError())
        }
        .make
        .toOption
        .get
    )
    val throwCheck = runOnService(
      SimpleRestJsonBuilder
        .routes(HelloImpl)
        .middleware(middleware)
        .mapErrors { case _: MiddlewareException =>
          throw SpecificServerError()
        }
        .make
        .toOption
        .get
    )
    List(throwCheck, pureCheck).combineAll
  }

  test("server - middleware is applied") {
    serverMiddlewareTest(
      shouldFailInMiddleware = true,
      Request[IO](Method.POST, Uri.unsafeFromString("/bob")),
      response =>
        IO.pure(expect.eql(response.status, Status.InternalServerError))
    )
  }

  test(
    "server - middleware allows passing through to underlying implementation"
  ) {
    serverMiddlewareTest(
      shouldFailInMiddleware = false,
      Request[IO](Method.POST, Uri.unsafeFromString("/bob")),
      response => {
        response.body.compile
          .to(Collector.supportsArray(Array))
          .map(new String(_))
          .map { body =>
            expect.eql(response.status, Status.Ok) &&
            expect.eql(body, """{"message":"Hello, bob"}""")
          }
      }
    )
  }

  test("client - middleware is applied") {
    clientMiddlewareTest(
      shouldFailInMiddleware = true,
      service =>
        service.hello("bob").attempt.map { result =>
          expect.eql(result, Left(new GenericServerError(Some("failed"))))
        }
    )
  }

  test("client - send request through middleware") {
    clientMiddlewareTest(
      shouldFailInMiddleware = false,
      service =>
        service.hello("bob").attempt.map { result =>
          expect.eql(result, Right(Greeting("Hello, bob")))
        }
    )
  }

  private def serverMiddlewareTest(
      shouldFailInMiddleware: Boolean,
      request: Request[IO],
      expect: Response[IO] => IO[Expectations]
  )(implicit pos: SourceLocation): IO[Expectations] = {
    val service =
      SimpleRestJsonBuilder
        .routes(HelloImpl)
        .middleware(
          new TestServerMiddleware(shouldFail = shouldFailInMiddleware)
        )
        .make
        .toOption
        .get

    service(request)
      .flatMap(res => OptionT.liftF(expect(res)))
      .getOrElse(
        failure("unable to run request")
      )
  }

  private def clientMiddlewareTest(
      shouldFailInMiddleware: Boolean,
      expect: HelloWorldService[IO] => IO[Expectations]
  ): IO[Expectations] = {
    val serviceNoMiddleware: HttpApp[IO] =
      SimpleRestJsonBuilder
        .routes(HelloImpl)
        .make
        .toOption
        .get
        .orNotFound

    val client: HelloWorldService[IO] = {
      val http4sClient = Client.fromHttpApp(serviceNoMiddleware)
      SimpleRestJsonBuilder(HelloWorldService)
        .client(http4sClient)
        .middleware(
          new TestClientMiddleware(shouldFail = shouldFailInMiddleware)
        )
        .use
        .toOption
        .get
    }

    expect(client)
  }

  private object HelloImpl extends HelloWorldService[IO] {
    def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure(
      Greeting(s"Hello, $name")
    )
  }

  private final class TestServerMiddleware(shouldFail: Boolean)
      extends ServerEndpointMiddleware.Simple[IO] {
    def prepareWithHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[IO] => HttpApp[IO] = { inputApp =>
      HttpApp[IO] { request =>
        val hasTag: (Hints, String) => Boolean = (hints, tagName) =>
          hints.get[smithy.api.Tags].exists(_.value.contains(tagName))
        // check for tags in hints to test that proper hints are sent into the prepare method
        if (
          hasTag(serviceHints, "testServiceTag") &&
          hasTag(endpointHints, "testOperationTag")
        ) {
          if (shouldFail) {
            IO.raiseError(new GenericServerError(Some("failed")))
          } else {
            inputApp(request)
          }
        } else {
          IO.raiseError(new Exception("didn't find tags in hints"))
        }
      }
    }
  }

  private final class TestClientMiddleware(shouldFail: Boolean)
      extends ClientEndpointMiddleware.Simple[IO] {
    def prepareWithHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): Client[IO] => Client[IO] = { inputClient =>
      Client[IO] { request =>
        val hasTag: (Hints, String) => Boolean = (hints, tagName) =>
          hints.get[smithy.api.Tags].exists(_.value.contains(tagName))
        // check for tags in hints to test that proper hints are sent into the prepare method
        if (
          hasTag(serviceHints, "testServiceTag") &&
          hasTag(endpointHints, "testOperationTag")
        ) {
          if (shouldFail) {
            Resource.eval(IO.raiseError(new GenericServerError(Some("failed"))))
          } else {
            inputClient.run(request)
          }
        } else {
          Resource.eval(
            IO.raiseError(new Exception("didn't find tags in hints"))
          )
        }
      }
    }
  }

}
