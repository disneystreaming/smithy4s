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
package internals

import cats.implicits._
import cats.effect.Temporal
import cats.effect.syntax.all._
import org.http4s.headers.`Content-Type`
import org.http4s.HttpApp
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.Uri
import smithy.test._
import smithy4s.compliancetests.ComplianceTest.ComplianceResult
import smithy4s.Document
import smithy4s.http.HttpContractError
import smithy4s.Service
import cats.Eq

import scala.concurrent.duration._
import org.http4s.MediaType
import org.http4s.Headers
import smithy4s.schema.Alt

private[compliancetests] class ClientHttpComplianceTestCase[
    F[_],
    Alg[_[_, _, _, _, _]]
](
    reverseRouter: ReverseRouter[F],
    serviceInstance: Service[Alg]
)(implicit ce: Temporal[F]) {
  import ce._
  import org.http4s.implicits._
  import reverseRouter._
  private val baseUri = uri"http://localhost/"
  private[compliancetests] implicit val service: Service[Alg] = serviceInstance

  private def matchRequest(
      request: Request[F],
      testCase: HttpRequestTestCase
  ): F[ComplianceResult] = {

    val bodyAssert = testCase.body
      .map { expectedBody =>
        request.bodyText.compile.string.map { responseBody =>
          assert.bodyEql(responseBody, expectedBody, testCase.bodyMediaType)

        }
      }
      .getOrElse(assert.success.pure[F])

    val expectedUri = baseUri
      .withPath(
        Uri.Path.unsafeFromString(testCase.uri)
      )
      .withMultiValueQueryParams(
        parseQueryParams(testCase.queryParams)
      )

    val pathAssert =
      assert.eql(expectedUri.path.renderString, request.uri.path.renderString)
    val queryAssert = assert.eql(
      expectedUri.query.renderString,
      request.uri.query.renderString
    )
    val methodAssert = assert.eql(
      testCase.method.toLowerCase(),
      request.method.name.toLowerCase()
    )
    val ioAsserts: List[F[ComplianceResult]] = bodyAssert +:
      List(
        assert.testCase.checkHeaders(testCase, request.headers),
        pathAssert,
        queryAssert,
        methodAssert
      )
        .map(_.pure[F])
    ioAsserts.combineAll(cats.Applicative.monoid[F, ComplianceResult])
  }

  private[compliancetests] def clientRequestTest[I, E, O, SE, SO](
      endpoint: service.Endpoint[I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[F] = {
    type R[I_, E_, O_, SE_, SO_] = F[O_]

    val revisedSchema = mapAllTimestampsToEpoch(endpoint.input.awsHintMask)
    val inputFromDocument = Document.Decoder.fromSchema(revisedSchema)
    ComplianceTest[F](
      name = endpoint.id.toString + "(client|request): " + testCase.id,
      run = {
        val input = inputFromDocument
          .decode(testCase.params.getOrElse(Document.obj()))
          .liftTo[F]

        deferred[Request[F]].flatMap { requestDeferred =>
          val app = HttpApp[F] { req =>
            req.body.compile.toVector
              .map(fs2.Stream.emits(_))
              .map(req.withBodyStream(_))
              .flatMap(requestDeferred.complete(_))
              .as(Response[F]())
          }

          reverseRoutes[Alg](app).use { client =>
            input
              .flatMap { in =>
                // avoid blocking the test forever...
                val request = requestDeferred.get.timeout(1.second)
                val output: F[O] = service
                  .toPolyFunction[R](client)
                  .apply(endpoint.wrap(in))
                output.attemptNarrow[HttpContractError].productR(request)
              }
              .flatMap { req => matchRequest(req, testCase) }
          }
        }
      }
    )
  }

  private[compliancetests] def clientResponseTest[I, E, O, SE, SO](
      endpoint: service.Endpoint[I, E, O, SE, SO],
      testCase: HttpResponseTestCase,
      errorSchema: Option[ErrorResponseTest[_, E]] = None
  ): ComplianceTest[F] = {

    type R[I_, E_, O_, SE_, SO_] = F[O_]

    val dummyInput = DefaultSchemaVisitor(endpoint.input)

    ComplianceTest[F](
      name = endpoint.id.toString + "(client|response): " + testCase.id,
      run = {
        val revisedSchema = mapAllTimestampsToEpoch(endpoint.output.awsHintMask)
        implicit val outputEq: Eq[O] =
          smithy4s.compliancetests.internals.eq.EqSchemaVisitor(revisedSchema)
        val buildResult = {
          errorSchema
            .toLeft {
              val outputDecoder = Document.Decoder.fromSchema(revisedSchema)
              (doc: Document) =>
                outputDecoder
                  .decode(doc)
                  .liftTo[F]
            }
            .left
            .map(_.errorEq[F])
        }
        val mt = expectedResponseType(endpoint.output)
        val status = Status.fromInt(testCase.code).liftTo[F]

        status.flatMap { status =>
          val app = HttpApp[F] { req =>
            val body: fs2.Stream[F, Byte] =
              testCase.body
                .map { body =>
                  fs2.Stream
                    .emit(body)
                    .through(fs2.text.utf8.encode)
                }
                .getOrElse(fs2.Stream.empty)

            val headers = Headers(
              `Content-Type`(MediaType.unsafeParse(mt.value))
            ) ++ parseHeaders(testCase.headers)

            req.body.compile.drain.as(
              Response[F](status)
                .withBodyStream(body)
                .withHeaders(headers)
            )
          }

          reverseRoutes[Alg](app).use { client =>
            val doc = testCase.params.getOrElse(Document.obj())
            buildResult match {
              case Left(onError) =>
                val res: F[O] = service
                  .toPolyFunction[R](client)
                  .apply(endpoint.wrap(dummyInput))
                res
                  .map { _ => assert.success }
                  .recoverWith { case ex: Throwable => onError(doc, ex) }
              case Right(onOutput) =>
                onOutput(doc).flatMap { expectedOutput =>
                  val res: F[O] = service
                    .toPolyFunction[R](client)
                    .apply(endpoint.wrap(dummyInput))
                  res.map { output =>
                    assert.eql(
                      expectedOutput,
                      output
                    )
                  }
                }
            }
          }
        }
      }
    )
  }

  def allClientTests(): List[ComplianceTest[F]] = {
    service.endpoints.flatMap { case endpoint =>
      val requestTests = endpoint.hints
        .get(HttpRequestTests)
        .map(_.value)
        .getOrElse(Nil)
        .filter(_.protocol == protocolTag.id.toString())
        .filter(tc => tc.appliesTo.forall(_ == AppliesTo.CLIENT))
        .map(tc => clientRequestTest(endpoint, tc))

      val opResponseTests = endpoint.hints
        .get(HttpResponseTests)
        .map(_.value)
        .getOrElse(Nil)
        .filter(_.protocol == protocolTag.id.toString())
        .filter(tc => tc.appliesTo.forall(_ == AppliesTo.CLIENT))
        .map(tc => clientResponseTest(endpoint, tc))

      val errorResponseTests = endpoint.errorable.toList
        .flatMap { errorrable =>
          errorrable.error.alternatives.flatMap { errorAlt =>
            errorAlt.instance.hints
              .get(HttpResponseTests)
              .toList
              .flatMap(_.value)
              .filter(_.protocol == protocolTag.id.toString())
              .filter(tc => tc.appliesTo.forall(_ == AppliesTo.SERVER))
              .map(tc =>
                clientResponseTest(
                  endpoint,
                  tc,
                  errorSchema = Some(
                    ErrorResponseTest
                      .from(
                        errorAlt,
                        Alt.Dispatcher(
                          errorrable.error.alternatives,
                          errorrable.error.dispatch(_)
                        ),
                        errorrable
                      )
                  )
                )
              )
          }
        }

      requestTests ++ opResponseTests ++ errorResponseTests
    }
  }
}
