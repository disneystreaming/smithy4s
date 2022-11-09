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

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import org.http4s.HttpApp
import org.http4s.headers.`Content-Type`
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.Uri
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.compliancetests.ComplianceTest.ComplianceResult
import smithy4s.http.CodecAPI
import smithy4s.Document
import smithy4s.http.PayloadError
import smithy4s.Service
import smithy4s.ShapeTag
import smithy4s.kinds._
import smithy4s.tests.DefaultSchemaVisitor

import scala.concurrent.duration._
import smithy4s.http.HttpMediaType
import org.http4s.MediaType
import org.http4s.Header

abstract class ClientHttpComplianceTestCase[
    P,
    Alg[_[_, _, _, _, _]]
](
    protocol: P,
    serviceProvider: Service.Provider[Alg]
)(implicit
    ce: CompatEffect,
    protocolTag: ShapeTag[P]
) {
  import ce._
  import org.http4s.implicits._
  private val baseUri = uri"http://localhost/"
  private[compliancetests] val service = serviceProvider.service

  def getClient(app: HttpApp[IO]): Resource[IO, FunctorAlgebra[Alg, IO]]
  def codecs: CodecAPI

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
    val ioAsserts = bodyAssert +:
      List(
        assert.testCase.checkHeaders(testCase, request.headers),
        uriAssert,
        methodAssert
      )
        .map(_.pure[IO])
    ioAsserts.combineAll
  }

  private[compliancetests] def clientRequestTest[I, E, O, SE, SO](
      endpoint: service.Endpoint[I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[IO] = {
    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)
    ComplianceTest[IO](
      name = endpoint.id.toString + "(client|request): " + testCase.id,
      run = {
        val input = inputFromDocument
          .decode(testCase.params.getOrElse(Document.obj()))
          .liftTo[IO]

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
                  .toPolyFunction[R](client)
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

  private[compliancetests] def clientResponseTest[I, E, O, SE, SO](
      endpoint: service.Endpoint[I, E, O, SE, SO],
      testCase: HttpResponseTestCase,
      errorSchema: Option[ErrorResponseTest[_, E]] = None
  ): ComplianceTest[IO] = {
    def aMediatype[A](
        s: smithy4s.Schema[A],
        cd: CodecAPI
    ): HttpMediaType = {
      cd.mediaType(cd.compileCodec(s))
    }

    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    val dummyInput = DefaultSchemaVisitor(endpoint.input)

    ComplianceTest[IO](
      name = endpoint.id.toString + "(client|response): " + testCase.id,
      run = {

        val buildResult
            : Either[Document => IO[Throwable], Document => IO[O]] = {
          errorSchema
            .toLeft {
              val outputDecoder = Document.Decoder.fromSchema(endpoint.output)
              (doc: Document) =>
                outputDecoder
                  .decode(doc)
                  .liftTo[IO]
            }
            .left
            .map { errorInfo =>
              val errorDecoder = Document.Decoder.fromSchema(errorInfo.schema)
              (doc: Document) =>
                errorDecoder
                  .decode(doc)
                  .liftTo[IO]
                  .map(errCase =>
                    errorInfo.errorable.unliftError(errCase.asInstanceOf[E])
                  )
            }
        }
        val mediaType = aMediatype(endpoint.output, codecs)
        val status = Status.fromInt(testCase.code).liftTo[IO]

        status.flatMap { status =>
          val app = HttpRoutes
            .of[IO] { case req =>
              val body: fs2.Stream[IO, Byte] =
                testCase.body
                  .map { body =>
                    fs2.Stream
                      .emit(body)
                      .through(utf8Encode)
                  }
                  .getOrElse(fs2.Stream.empty)
              val headers: Seq[Header.ToRaw] =
                testCase.headers.toList
                  .flatMap(_.toList)
                  .map { case (key, value) =>
                    Header.Raw(CIString(key), value)
                  }
                  .map(Header.ToRaw.rawToRaw)
                  .toSeq
              req.body.compile.drain.as(
                Response[IO](status)
                  .withBodyStream(body)
                  .putHeaders(headers: _*)
                  .putHeaders(
                    `Content-Type`(MediaType.unsafeParse(mediaType.value))
                  )
              )
            }
            .orNotFound

          getClient(app).use { client =>
            val doc = testCase.params.getOrElse(Document.obj())
            buildResult match {
              case Left(onError) =>
                onError(doc).flatMap { expectedErr =>
                  service
                    .toPolyFunction[R](client)
                    .apply(endpoint.wrap(dummyInput))
                    .map { _ => assert.success }
                    .recover { case ex: Throwable =>
                      assert.eql(expectedErr, ex)
                    }
                }
              case Right(onOutput) =>
                onOutput(doc).flatMap { expectedOutput =>
                  service
                    .toPolyFunction[R](client)
                    .apply(endpoint.wrap(dummyInput))
                    .map { output => assert.eql(expectedOutput, output) }
                }
            }
          }
        }
      }
    )
  }

  def allClientTests(
  ): List[ComplianceTest[IO]] = {
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
                  errorSchema =
                    Some(ErrorResponseTest(errorAlt.instance, errorrable))
                )
              )
          }
        }

      requestTests ++ opResponseTests ++ errorResponseTests
    }
  }
}
