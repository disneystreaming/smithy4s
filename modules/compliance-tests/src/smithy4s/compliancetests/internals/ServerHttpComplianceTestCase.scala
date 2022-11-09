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

import java.nio.charset.StandardCharsets

import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.http4s.headers.`Content-Type`
import smithy.test._
import smithy4s.Document
import smithy4s.Service
import smithy4s.kinds._

import scala.concurrent.duration._
import smithy4s.ShapeId
import smithy4s.Hints
import smithy4s.Errorable

private[compliancetests] class ServerHttpComplianceTestCase[
    Alg[_[_, _, _, _, _]]
](
    router: Router[IO],
    serviceProvider: Service.Provider[Alg]
)(implicit
    ce: CompatEffect[IO]
) {
  import ce._
  import org.http4s.implicits._
  import router._
  private[compliancetests] val originalService = serviceProvider.service
  private val baseUri = uri"http://localhost/"

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

    val body =
      testCase.body
        .map(b => fs2.Stream.emit(b).through(ce.utf8Encode))
        .getOrElse(fs2.Stream.empty)

    Request[IO](
      method = expectedMethod,
      uri = expectedUri,
      headers = expectedHeaders,
      body = body
    )
  }

  private[compliancetests] def serverRequestTest[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[IO] = {

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)
    ComplianceTest[IO](
      name = endpoint.id.toString + "(server|request): " + testCase.id,
      run = {
        deferred[I].flatMap { inputDeferred =>
          val fakeImpl: FunctorAlgebra[Alg, IO] =
            originalService.fromPolyFunction[Kind1[IO]#toKind5](
              new originalService.FunctorInterpreter[IO] {
                def apply[I_, E_, O_, SE_, SO_](
                    op: originalService.Operation[I_, E_, O_, SE_, SO_]
                ): IO[O_] = {
                  val (in, endpointInternal) = originalService.endpoint(op)

                  if (endpointInternal.id == endpoint.id)
                    inputDeferred.complete(in.asInstanceOf[I]) *>
                      IO.raiseError(new NotImplementedError)
                  else IO.raiseError(new Throwable("Wrong endpoint called"))
                }
              }
            )

          routes(fakeImpl)(originalService)
            .use { server =>
              server.orNotFound
                .run(makeRequest(baseUri, testCase))
                .attemptNarrow[NotImplementedError] *>
                inputDeferred.get.timeout(1.second).flatMap { foundInput =>
                  inputFromDocument
                    .decode(testCase.params.getOrElse(Document.obj()))
                    .liftTo[IO]
                    .map { decodedInput =>
                      assert.eql(foundInput, decodedInput)
                    }
                }
            }
        }
      }
    )
  }

  private[compliancetests] def serverResponseTest[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO],
      testCase: HttpResponseTestCase,
      errorSchema: Option[ErrorResponseTest[_, E]] = None
  ): ComplianceTest[IO] = {

    ComplianceTest[IO](
      name = endpoint.id.toString + "(server|response): " + testCase.id,
      run = {
        val (ammendedService, syntheticRequest) = prepareService(endpoint)

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

        val fakeImpl: FunctorInterpreter[NoInputOp, IO] =
          new FunctorInterpreter[NoInputOp, IO] {
            def apply[I_, E_, O_, SE_, SO_](
                op: NoInputOp[I_, E_, O_, SE_, SO_]
            ): IO[O_] = {
              val doc = testCase.params.getOrElse(Document.obj())
              buildResult match {
                case Left(onError) =>
                  onError(doc).flatMap { err =>
                    IO.raiseError[O_](err)
                  }
                case Right(onOutput) =>
                  onOutput(doc).map(_.asInstanceOf[O_])
              }
            }
          }

        routes(fakeImpl)(ammendedService)
          .use { server =>
            server.orNotFound
              .run(syntheticRequest)
              .flatMap { resp =>
                resp.body
                  .through(utf8Decode)
                  .compile
                  .foldMonoid
                  .tupleRight(resp.status)
                  .tupleRight(resp.headers)
              }
              .map { case ((actualBody, status), headers) =>
                val bodyAssert = testCase.body
                  .map(body => assert.eql(body, actualBody))
                val assertions =
                  bodyAssert.toList :+
                    assert.testCase.checkHeaders(testCase, headers) :+
                    assert.eql(status.code, testCase.code)
                assertions.combineAll
              }
          }
      }
    )
  }

  private case class NoInputOp[I_, E_, O_, SE_, SO_]()
  private def prepareService[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO]
  ): (Service.Reflective[NoInputOp], Request[IO]) = {
    val amendedEndpoint =
        // format: off
        new smithy4s.Endpoint[NoInputOp, Unit, E, O, Nothing, Nothing] {
          def hints: smithy4s.Hints = {
            val newHttp = smithy.api.Http(
              method = smithy.api.NonEmptyString("GET"),
              uri = smithy.api.NonEmptyString("/")
            )
            val code = endpoint.hints.get[smithy.api.Http].map(_.code).getOrElse(newHttp.code)
            Hints(newHttp.copy(code = code))
          }
          def id: smithy4s.ShapeId = ShapeId("custom", "endpoint")
          def input: smithy4s.Schema[Unit] = smithy4s.Schema.unit
          def output: smithy4s.Schema[O] = endpoint.output
          def streamedInput: smithy4s.StreamingSchema[Nothing] =
            smithy4s.StreamingSchema.NoStream
          def streamedOutput: smithy4s.StreamingSchema[Nothing] =
            smithy4s.StreamingSchema.NoStream
          def wrap(input: Unit): NoInputOp[Unit, E, O, Nothing, Nothing] =
            NoInputOp()

          override def errorable: Option[Errorable[E]] = endpoint.errorable
        }
        // format: on
    val request = Request[IO](Method.GET, Uri.unsafeFromString("/"))
    val amendedService =
      // format: off
      new Service.Reflective[NoInputOp] {
        override def id: ShapeId = ShapeId("custom", "service")
        override def endpoints: List[Endpoint[_, _, _, _, _]] = List(amendedEndpoint)
        override def endpoint[I_, E_, O_, SI_, SO_](op: NoInputOp[I_, E_, O_, SI_, SO_]): (I_, Endpoint[I_, E_, O_, SI_, SO_]) = ???
        override def version: String = originalService.version
        override def hints: Hints = originalService.hints
      }
      // format: on
    (amendedService, request)
  }

  def allServerTests(): List[ComplianceTest[IO]] = {
    originalService.endpoints.flatMap { case endpoint =>
      val requestsTests = endpoint.hints
        .get(HttpRequestTests)
        .map(_.value)
        .getOrElse(Nil)
        .filter(_.protocol == protocolTag.id.toString())
        .filter(tc => tc.appliesTo.forall(_ == AppliesTo.SERVER))
        .map(tc => serverRequestTest(endpoint, tc))

      val opResponseTests = endpoint.hints
        .get(HttpResponseTests)
        .map(_.value)
        .getOrElse(Nil)
        .filter(_.protocol == protocolTag.id.toString())
        .filter(tc => tc.appliesTo.forall(_ == AppliesTo.SERVER))
        .map(tc => serverResponseTest(endpoint, tc))

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
                serverResponseTest(
                  endpoint,
                  tc,
                  errorSchema =
                    Some(ErrorResponseTest(errorAlt.instance, errorrable))
                )
              )
          }
        }

      requestsTests ++ opResponseTests ++ errorResponseTests
    }
  }
}
