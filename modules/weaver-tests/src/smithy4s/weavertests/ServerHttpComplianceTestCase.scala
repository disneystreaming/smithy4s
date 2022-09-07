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

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import cats.kernel.Eq
import org.http4s.client._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.Header
import org.http4s.Header
import org.http4s.Headers
import org.http4s.headers.`Content-Type`
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.http.PayloadError
import smithy4s.Service
import smithy4s.tests.DefaultSchemaVisitor
import smithy4s.UnsupportedProtocolError
import weaver._

import scala.concurrent.duration._

/**
  * Register unit test for client and server (when applicable) for @httpRequestTests
  * annotations.
  * 
  * For HttpResponseTestCase:
  * 1. The HttpResponseTestCase#params are used to generated an output, a dummy input is generated via the schema.
  * 2. The server implementation is setup to a) return the dummy output and b) record the outgoing response for assertion
  * 3. The dummy input is fed into the a client call (the operation under test). That
  *    client is hooked to the server setup at step 2. 
  * 4. Assertion are performed on the recorded response
  */
class ServerHttpComplianceTestCase[
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    getServer: smithy4s.Monadic[Alg, IO] => Resource[IO, (Client[IO], Uri)]
)(implicit service: Service[Alg, Op], ce: CompatEffect)
    extends Expectations.Helpers {
  import ce._
  type R[I_, E_, O_, SE_, SO_] = IO[O_]

  import org.http4s.implicits._

  private def makeRequest(
      baseUri: Uri,
      testCase: HttpRequestTestCase
  ): Request[IO] = {
    val expectedHeaders =
      List(
        testCase.headers.map(h =>
          Headers(h.toList.map(a => a: Header.ToRaw): _*)
        ),
        testCase.bodyMediaType.map(mt =>
          Headers(`Content-Type`(MediaType.unsafeParse(mt)))
        )
      ).foldMap(_.combineAll)

    val expectedMethod = Method
      .fromString(testCase.method)
      .getOrElse(sys.error("Invalid method"))

    val expectedUri = baseUri
      .withPath(
        Uri.Path.unsafeFromString(testCase.uri).addEndsWithSlash
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

    val expectedBody =
      testCase.body.getOrElse(sys.error("no body expectation: todo"))

    Request[IO](
      method = expectedMethod,
      uri = expectedUri,
      headers = expectedHeaders,
      body = fs2.Stream.emit(expectedBody).through(fs2.text.utf8Encode[IO])
    )
  }

  private[weavertests] def makeServerTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ): GeneratedTest = {
    GeneratedTest(
      name = endpoint.id.toString + "(server|request): " + testCase.id,
      assertions = {
        val expectedInput = testCase.params
          .map { inputFromDocument.decode(_).liftTo[IO] }
          .getOrElse(IO.pure(().asInstanceOf[I]))
        (deferred[I], expectedInput).tupled
          .flatMap { case (requestDeferred, input) =>
            val fakeImpl: smithy4s.Monadic[Alg, IO] =
              service.transform[R](
                new smithy4s.Interpreter[Op, IO] {
                  def apply[I_, E_, O_, SE_, SO_](
                      op: Op[I_, E_, O_, SE_, SO_]
                  ): IO[O_] = {
                    val (in, endpointInternal) = service.endpoint(op)

                    if (endpointInternal.id == endpoint.id)
                      requestDeferred.complete(in.asInstanceOf[I]) *>
                        IO.raiseError(new NotImplementedError)
                    else IO.raiseError(new Throwable("Wrong endpoint called"))
                  }
                }
              )

            val recordedInput = requestDeferred.get.timeout(1.second)
            getServer(fakeImpl)
              .use { case (client, uri) =>
                client
                  .run(makeRequest(uri, testCase))
                  .use { _ => IO.unit }
                  .attemptNarrow[NotImplementedError]
              }
              .productR(recordedInput)
              .map { actualInput =>
                assert.same(input, actualInput)
              }
          }
      }
    )
  }

}
