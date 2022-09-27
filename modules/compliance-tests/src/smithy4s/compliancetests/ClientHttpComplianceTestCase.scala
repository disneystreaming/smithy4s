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

package smithy4s.compliancetests

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
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

import ComplianceTest.ComplianceResult

import java.nio.charset.StandardCharsets

import concurrent.duration._
import org.http4s.Response
import smithy4s.ShapeTag
import org.http4s.ember.server.EmberServerBuilder

class Http4sClientHttpComplianceTestCase[
    P,
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    protocol: P,
    makeClient: Either[
      // make an in-memory client
      HttpApp[IO] => Resource[IO, smithy4s.Monadic[Alg, IO]],
      // start a server, return a client that calls it
      Int => Resource[IO, smithy4s.Monadic[Alg, IO]]
    ]
)(implicit
    service: Service[Alg, Op],
    ce: CompatEffect,
    protocolTag: ShapeTag[P]
) extends ClientHttpComplianceTestCase(protocol) {

  import ce._
  locally(deferred) // to disable unused warning on ce3

  private val randomInt =
    Resource.eval(IO(scala.util.Random.nextInt(9999)))

  private val randomPort = randomInt.map(_ + 50000)

  private def retryResource[A](
      resource: Resource[IO, A],
      max: Int = 10
  ): Resource[IO, A] =
    if (max <= 0) resource
    else resource.orElse(retryResource(resource, max - 1))

  def getClient(app: HttpApp[IO]): Resource[IO, smithy4s.Monadic[Alg, IO]] = {
    makeClient.fold(
      f => f(app),
      makeClientFromPort =>
        retryResource {
          randomPort
            .flatTap { port =>
              EmberServerBuilder
                .default[IO]
                .withHost(Compat.host("localhost"))
                .withPort(Compat.port(port))
                .withHttpApp(app)
                .build
            }
        }.flatMap(makeClientFromPort)
    )
  }
}

abstract class ClientHttpComplianceTestCase[
    P,
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    protocol: P
)(implicit
    service: Service[Alg, Op],
    ce: CompatEffect,
    protocolTag: ShapeTag[P]
) {
  import ce._
  import org.http4s.implicits._
  private val baseUri = uri"http://localhost/"

  def getClient(app: HttpApp[IO]): Resource[IO, smithy4s.Monadic[Alg, IO]]

  private def matchRequest(
      request: Request[IO],
      testCase: HttpRequestTestCase
  ): IO[ComplianceResult] = {
    val bodyAssert = testCase.body
      .map { expectedBody =>
        request.bodyText.compile.string.map { responseBody =>
          assert.eql(expectedBody, responseBody)
        }
      }
      .getOrElse(assert.success.pure[IO])

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
                  assert.fail(s"Header $name was not found in the response.")
                )
            }
            .toList
            .combineAll
        }
        .getOrElse(assert.success)

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

  private[compliancetests] def clientRequestTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[IO] = {
    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)
    ComplianceTest[IO](
      name = endpoint.id.toString + "(client|request): " + testCase.id,
      run = {
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

          getClient(app).use { client =>
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

  def allClientTests(
  ): List[ComplianceTest[IO]] = {
    service.endpoints.flatMap { case endpoint =>
      endpoint.hints
        .get(HttpRequestTests)
        .map(_.value)
        .getOrElse(Nil)
        .filter(_.protocol == protocolTag.id.toString())
        .filter(tc => tc.appliesTo.forall(_ == AppliesTo.CLIENT))
        .map(tc => clientRequestTest(endpoint, tc))
    }
  }
}
