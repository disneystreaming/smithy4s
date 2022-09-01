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
import cats.implicits._
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.Uri
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.Service
import smithy4s.UnsupportedProtocolError
import smithy4s.tests.DefaultSchemaVisitor
import weaver._

import concurrent.duration._

/**
  * Register unit test for client and server (when applicable) for @httpResponseTests
  * annotations.
  * 
  * For the client:
  * 1. The HttpRequestTestCase#params are used to generated an output, a dummy input is generated via the schema.
  * 2. The server implementation is setup to a)return a dummy output, and b) record
  *    the incoming request for assertion
  * 3. This input is fed into a genuine client call (the operation under test). This client is hooked to the server setip at 2)
  * 4. Assertion are performed on the resulting output
  * 
  * For the server:
  * 1. The HttpRequestTestCase#params are used to generated an output, a dummy input is generated via the schema.
  * 2. The server implementation is setup to a) return the dummy output and b) record the outgoing response for assertion
  * 3. The dummy input is fed into the a client call (the operation under test). That
  *    client is hooked to the server setup at step 2. 
  * 4. Assertion are performed on the recorded response
  *
  */
private class WeaverHttpResponseTestCase[
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
)(implicit service: Service[Alg, Op], ce: CompatEffect)
    extends Expectations.Helpers {
  import ce._
  import org.http4s.implicits._
  private val baseUri = uri"http://localhost/"

  type R[I_, E_, O_, SE_, SO_] = IO[O_]

  private def matchResponse(
      response: Response[IO],
      testCase: HttpResponseTestCase
  )(implicit loc: SourceLocation) = {
    val bodyAssert = testCase.body
      .map { expectedBody =>
        response.bodyText.compile.string.map { responseBody =>
          assert.eql(expectedBody, responseBody)
        }
      }
      .getOrElse(success.pure[IO])

    val headerAssert =
      testCase.headers
        .map { expectedHeaders =>
          expectedHeaders
            .map { case (name, value) =>
              val actual = response.headers.get(CIString(name))
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

    val statusAssert = assert.eql(testCase.code, response.status.code)

    List(
      bodyAssert,
      headerAssert.pure[IO],
      statusAssert.pure[IO]
    ).sequence.map(_.reduce(_ && _))
  }

  private[weavertests] def makeServerTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpResponseTestCase,
      outputFromDocument: Document.Decoder[O]
  ): GeneratedTest = GeneratedTest(
    name = endpoint.id.toString + "(server|response): " + testCase.id,
    assertions = {
      type R[I_, E_, O_, SE_, SO_] = IO[O_]

      val dummyInput = DefaultSchemaVisitor(endpoint.input)
      val output =
        testCase.params
          .map {
            outputFromDocument
              .decode(_)
              .liftTo[IO]
          }
          .getOrElse(IO.unit.asInstanceOf[IO[O]])

      deferred[Response[IO]]
        .flatMap { responseDeferred =>
          val mockedImpl: smithy4s.Monadic[Alg, IO] =
            service.transform[R](
              new smithy4s.Interpreter[Op, IO] {
                def apply[I_, E_, O_, SE_, SO_](
                    op: Op[I_, E_, O_, SE_, SO_]
                ): IO[O_] = output.map(_.asInstanceOf[O_])
              }
            )
          server(mockedImpl)
            .liftTo[IO]
            .map(_.orNotFound)
            .flatMap { httpApp =>
              val captureApp = HttpApp[IO] { req =>
                httpApp
                  .run(req)
                  .flatTap { resp =>
                    resp.body.compile.toVector
                      .map(fs2.Stream.emits(_))
                      .map(resp.withBodyStream(_))
                      .flatMap(responseDeferred.complete(_))
                      .as(resp)
                  }
              }
              client(captureApp, baseUri)
                .liftTo[IO]
                .flatMap { client =>
                  service
                    .asTransformation[R](client)
                    .apply(endpoint.wrap(dummyInput))
                }
            } *>
            responseDeferred.get.timeout(1.second).flatMap { recordedResponse =>
              matchResponse(recordedResponse, testCase)
            }
        }
    }
  )

  private[weavertests] def makeClientTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpResponseTestCase,
      outputFromDocument: Document.Decoder[O]
  ): GeneratedTest = GeneratedTest(
    name = endpoint.id.toString + "(client|response): " + testCase.id,
    assertions = {
      type R[I_, E_, O_, SE_, SO_] = IO[O_]

      val dummyInput = DefaultSchemaVisitor(endpoint.input)

      val output = testCase.params
        .map { outputFromDocument.decode(_).liftTo[IO] }
        .getOrElse(IO.unit.asInstanceOf[IO[O]])

      val mockedImpl: smithy4s.Monadic[Alg, IO] =
        service.transform[R](
          new smithy4s.Interpreter[Op, IO] {
            def apply[I_, E_, O_, SE_, SO_](
                op: Op[I_, E_, O_, SE_, SO_]
            ): IO[O_] = output.map(_.asInstanceOf[O_])
          }
        )
      val mockedRoutes = server(mockedImpl).map(_.orNotFound).liftTo[IO]

      (output, mockedRoutes).tupled
        .flatMap { case (expectedO, routes) =>
          val theClient: IO[smithy4s.Monadic[Alg, IO]] = client(
            HttpApp[IO] { routes.run },
            baseUri
          ).liftTo[IO]

          theClient
            .flatMap { client =>
              service
                .asTransformation[R](client)
                .apply(endpoint.wrap(dummyInput))
            }
            .map { actualO =>
              assert.same(expectedO, actualO)
            }
        }
    }
  )
}
