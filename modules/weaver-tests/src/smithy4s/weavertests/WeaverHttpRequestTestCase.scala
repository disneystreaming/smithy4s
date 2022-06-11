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
import cats.effect.kernel.Deferred
import cats.implicits._
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Uri
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.Service
import smithy4s.UnsupportedProtocolError
import smithy4s.tests.DefaultSchemaVisitor
import weaver._

import java.nio.charset.StandardCharsets

import concurrent.duration._

/**
  * Register unit test for client and server (when applicable) for @httpRequestTests
  * annotations.
  * 
  * For the client:
  * 1. The HttpRequestTestCase#params are used to generated an input, a dummy output is generated via the schema.
  * 2. The server implementation is setup to a)return a dummy output, and b) record
  *    the incoming request for assertion
  * 3. This input is fed into a genuine client call (the operation under test). This client is hooked to the server setip at 2)
  * 4. Assertion are performed on the recorded request
  * 
  * For the server:
  * 1. The HttpRequestTestCase#params are used to generated an input, a dummy output is generated via the schema.
  * 2. The server implementation is setup to a) ensure the right endpoint is called, b) record
  *    the decoded input from the request and c)return a dummy output
  * 3. The generated input is fed into the a client call (the operation under test). That
  *    client is hooked to the server setup at step 2.
  * 4. Assertion are performed on the recorded input (from decoding the http request)
  *
  */
class WeaverHttpRequestTestCase[
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    client: (
        HttpApp[IO],
        Uri
    ) => Either[UnsupportedProtocolError, smithy4s.Monadic[Alg, IO]],
    server: smithy4s.Monadic[Alg, IO] => Either[
      UnsupportedProtocolError,
      HttpRoutes[IO]
    ]
)(implicit service: Service[Alg, Op])
    extends Expectations.Helpers {

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
            .reduce(_ && _)
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
    ).sequence.map(_.reduce(_ && _))
  }

  private[weavertests] def makeServerTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ): GeneratedTest = {
    GeneratedTest(
      name = endpoint.id.toString + "(server|request): " + testCase.id,
      assertions = {
        val dummyOutput = DefaultSchemaVisitor(endpoint.output)
        val input = testCase.params
          .map { inputFromDocument.decode(_).liftTo[IO] }
          .getOrElse(IO.unit.asInstanceOf[IO[I]])
        (Deferred[IO, I], input).tupled
          .flatMap { case (requestDeferred, input) =>
            type R[I_, E_, O_, SE_, SO_] = IO[O_]

            val fakeImpl: smithy4s.Monadic[Alg, IO] =
              service.transform[R](
                new smithy4s.Interpreter[Op, IO] {
                  def apply[I_, E_, O_, SE_, SO_](
                      op: Op[I_, E_, O_, SE_, SO_]
                  ): IO[O_] = {
                    val (in, endpointInternal) = service.endpoint(op)

                    if (endpointInternal.id == endpoint.id)
                      requestDeferred.complete(in.asInstanceOf[I]) *>
                        IO.pure(dummyOutput.asInstanceOf[O_])
                    else IO.raiseError(new Throwable("Wrong endpoint called"))
                  }
                }
              )

            server(fakeImpl)
              .liftTo[IO]
              .map(_.orNotFound)
              .flatMap { routes =>
                client(routes, baseUri)
                  .liftTo[IO]
                  .flatMap { client =>
                    service
                      .asTransformation[R](client)
                      .apply(endpoint.wrap(input))
                  }
              } *>
              requestDeferred.get.timeout(1.second).map { foundInput =>
                assert.same(input, foundInput)
              }
          }
      }
    )
  }

  private[weavertests] def makeClientTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ): GeneratedTest = {
    GeneratedTest(
      name = endpoint.id.toString + "(client|request): " + testCase.id,
      assertions = {
        val dummyOutput = DefaultSchemaVisitor(endpoint.output)
        val input = testCase.params
          .map { inputFromDocument.decode(_).liftTo[IO] }
          .getOrElse(IO.unit.asInstanceOf[IO[I]])

        val fakeImpl: smithy4s.Monadic[Alg, IO] =
          service.transform[R](
            new smithy4s.Interpreter[Op, IO] {
              def apply[I_, E_, O_, SE_, SO_](
                  op: Op[I_, E_, O_, SE_, SO_]
              ): IO[O_] = {
                IO.pure(dummyOutput.asInstanceOf[O_])
              }
            }
          )

        val mockedRoutes = server(fakeImpl).map(_.orNotFound).liftTo[IO]
        type R[I_, E_, O_, SE_, SO_] = IO[O_]

        (Deferred[IO, Request[IO]], mockedRoutes).tupled
          .flatMap { case (requestDeferred, routes) =>
            val theClient: IO[smithy4s.Monadic[Alg, IO]] = client(
              HttpApp[IO] { req =>
                // Save consumed stream for later reuse
                req.body.compile.toVector
                  .map(fs2.Stream.emits(_))
                  .map(req.withBodyStream(_))
                  .flatMap(requestDeferred.complete(_)) *> routes.run(req)
              },
              baseUri
            ).liftTo[IO]

            theClient
              .flatMap { client =>
                input.flatMap { in =>
                  service
                    .asTransformation[R](client)
                    .apply(endpoint.wrap(in))
                }
              } *> requestDeferred.get
              // should be complete by now, but just to avoid blocking the test forever...
              .timeout(1.second)
          }
          .flatMap(matchRequest(_, testCase))
      }
    )
  }
}
