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

package smithy4s.weavertests

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Uri
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.Document
import smithy4s.http.PayloadError
import smithy4s.Endpoint
import smithy4s.Service
import weaver._

import java.nio.charset.StandardCharsets

import concurrent.duration._
import org.http4s.Response

/**
  * Register unit test for client and server (when applicable) for @httpRequestTests
  * annotations.
  * 
  * For HttpRequestTestCase:
  * 1. The HttpRequestTestCase#params are used to generated an input, a dummy output is generated via the schema.
  * 2. The server implementation is setup to a)return a dummy output, and b) record
  *    the incoming request for assertion
  * 3. This input is fed into a genuine client call (the operation under test). This client is hooked to the server setip at 2)
  * 4. Assertion are performed on the recorded request
  */
class ClientHttpComplianceTestCase[
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    getClient: Either[
      HttpApp[IO] => Resource[IO, smithy4s.Monadic[Alg, IO]],
      Int => Resource[IO, smithy4s.Monadic[Alg, IO]]
    ]
)(implicit service: Service[Alg, Op], ce: CompatEffect)
    extends Expectations.Helpers {
  import ce._
  import org.http4s.implicits._
  private val baseUri = uri"http://localhost/"

  private def matchRequest(
      request: Request[IO],
      testCase: HttpRequestTestCase
  )(implicit loc: SourceLocation) = {
    val bodyAssert = testCase.body
      .map { expectedBody =>
        request.bodyText.compile.string.map { responseBody =>
          assert.eql(expectedBody, responseBody)
        }
      }
      .getOrElse(success.pure[IO])

    val headerAssert =
      testCase.headers
        .map { expectedHeaders =>
          expectedHeaders
            .map { case (name, value) =>
              val actual = request.headers.get(CIString(name))
              actual
                .map { values =>
                  val actualValue = values.map(_.value).fold
                  assert.eql(value, actualValue)
                }
                .getOrElse(
                  failure(s"Header $name was not found in the response.")
                )
            }
            .toList
            .combineAll
        }
        .getOrElse(success)

    val expectedUri = baseUri
      .withPath(
        Uri.Path.unsafeFromString(testCase.uri)
      )
      .withQueryParams(
        testCase.queryParams.combineAll.map {
          _.split("=", 2) match {
            case Array(k, v) =>
              (
                k,
                Uri.decode(
                  toDecode = v,
                  charset = StandardCharsets.UTF_8,
                  plusIsSpace = true
                )
              )
          }
        }.toMap
      )

    val uriAssert = assert.eql(expectedUri, request.uri)
    val methodAssert = assert.eql(
      testCase.method.toLowerCase(),
      request.method.name.toLowerCase()
    )

    List(
      bodyAssert,
      headerAssert.pure[IO],
      uriAssert.pure[IO],
      methodAssert.pure[IO]
    ).combineAll
  }

  private[weavertests] def clientRequestTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ): GeneratedTest = {
    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    GeneratedTest(
      name = endpoint.id.toString + "(client|request): " + testCase.id,
      assertions = {
        val input = testCase.params
          .map { inputFromDocument.decode(_).liftTo[IO] }
          .getOrElse(IO.pure(().asInstanceOf[I]))

        deferred[Request[IO]].flatMap { requestDeferred =>
          val app = HttpRoutes
            .of[IO] { case req =>
              req.body.compile.toVector
                .map(fs2.Stream.emits(_))
                .map(req.withBodyStream(_))
                .flatMap(requestDeferred.complete(_))
                .as(Response[IO]())
            }
            .orNotFound

          val clientRes = getClient match {
            case Left(fromApp) => fromApp(app)
            case Right(fromPort) =>
              for {
                port <- retryResource(emberServer(app))
                client <- fromPort(port)
              } yield client
          }

          clientRes.use { client =>
            // avoid blocking the test forever...
            val recordedRequest = requestDeferred.get.timeout(1.second)

            input
              .flatMap { in =>
                service
                  .asTransformation[R](client)
                  .apply(endpoint.wrap(in))
              }
              // deal with the empty response generated in the mock
              .attemptNarrow[PayloadError]
              .productR(recordedRequest)
              .flatMap(matchRequest(_, testCase))
          }
        }
      }
    )
  }

  private val randomInt =
    Resource.eval(IO(scala.util.Random.nextInt(9999)))

  private val randomPort = randomInt.map(_ + 50000)

  private def emberServer(app: HttpApp[IO]): Resource[IO, Int] =
    randomPort.flatTap { port =>
      EmberServerBuilder
        .default[IO]
        .withHost(Compat.host("localhost"))
        .withPort(Compat.port(port))
        .withHttpApp(app)
        .build
    }

  private def retryResource[A](
      resource: Resource[IO, A],
      max: Int = 10
  ): Resource[IO, A] =
    if (max <= 0) resource
    else resource.orElse(retryResource(resource, max - 1))
}
