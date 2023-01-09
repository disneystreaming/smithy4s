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
    F[_],
    Alg[_[_, _, _, _, _]]
](
    router: Router[F],
    serviceInstance: Service[Alg]
)(implicit
    ce: CompatEffect[F]
) {

  import ce._
  import org.http4s.implicits._
  import router._

  private[compliancetests] val originalService: Service[Alg] = serviceInstance
  private val baseUri = uri"http://localhost/"

  private def makeRequest(
      baseUri: Uri,
      testCase: HttpRequestTestCase
  ): Request[F] = {
    val expectedHeaders =
      testCase.bodyMediaType
        .map(mt => Headers(`Content-Type`(MediaType.unsafeParse(mt))))
        .getOrElse(Headers.empty) ++ parseHeaders(testCase.headers)

    val expectedMethod = Method
      .fromString(testCase.method)
      .getOrElse(sys.error("Invalid method"))

    val expectedUri = baseUri
      .withPath(
        Uri.Path.unsafeFromString(testCase.uri).addEndsWithSlash
      )
      .withMultiValueQueryParams(
        parseQueryParams(testCase.queryParams)
      )

    val body =
      testCase.body
        .map(b => fs2.Stream.emit(b).through(ce.utf8Encode))
        .getOrElse(fs2.Stream.empty)

    Request[F](
      method = expectedMethod,
      uri = expectedUri,
      headers = expectedHeaders,
      body = body
    )
  }

  private[compliancetests] def serverRequestTest[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[F] = {

    val revisedSchema = mapAllTimestampsToEpoch(endpoint.input.awsHintMask)
    val inputFromDocument = Document.Decoder.fromSchema(revisedSchema)
    ComplianceTest[F](
      name = endpoint.id.toString + "(server|request): " + testCase.id,
      run = {
        deferred[I].flatMap { inputDeferred =>
          val fakeImpl: FunctorAlgebra[Alg, F] =
            originalService.fromPolyFunction[Kind1[F]#toKind5](
              new originalService.FunctorInterpreter[F] {
                def apply[I_, E_, O_, SE_, SO_](
                    op: originalService.Operation[I_, E_, O_, SE_, SO_]
                ): F[O_] = {
                  val (in, endpointInternal) = originalService.endpoint(op)

                  if (endpointInternal.id == endpoint.id)
                    inputDeferred.complete(in.asInstanceOf[I]) *>
                      raiseError(new NotImplementedError)
                  else raiseError(new Throwable("Wrong endpoint called"))
                }
              }
            )

          routes(fakeImpl)(originalService)
            .use { server =>
              server.orNotFound
                .run(makeRequest(baseUri, testCase))
                .attemptNarrow[NotImplementedError]
                .flatMap {
                  case Left(_) =>
                    ce.timeout(inputDeferred.get, 1.second).flatMap {
                      foundInput =>
                        inputFromDocument
                          .decode(testCase.params.getOrElse(Document.obj()))
                          .liftTo[F]
                          .map { decodedInput =>
                            assert.eql(foundInput, decodedInput)
                          }
                    }
                  case Right(response) =>
                    response.body.compile.toVector.map { message =>
                      assert.fail(
                        s"Expected a NotImplementedError, but got a response with status ${response.status} and message ${message
                          .map(_.toChar)
                          .mkString}"
                      )
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
  ): ComplianceTest[F] = {

    ComplianceTest[F](
      name = endpoint.id.toString + "(server|response): " + testCase.id,
      run = {
        val (ammendedService, syntheticRequest) = prepareService(endpoint)

        val buildResult: Either[Document => F[Throwable], Document => F[O]] = {
          errorSchema
            .toLeft {
              val outputDecoder = Document.Decoder.fromSchema(
                mapAllTimestampsToEpoch(endpoint.output.awsHintMask)
              )
              (doc: Document) =>
                outputDecoder
                  .decode(doc)
                  .liftTo[F]
            }
            .left
            .map { errorInfo =>
              val errorDecoder = Document.Decoder.fromSchema(errorInfo.schema)
              (doc: Document) =>
                errorDecoder
                  .decode(doc)
                  .liftTo[F]
                  .map(errCase =>
                    errorInfo.errorable.unliftError(errCase.asInstanceOf[E])
                  )
            }
        }

        val fakeImpl: FunctorInterpreter[NoInputOp, F] =
          new FunctorInterpreter[NoInputOp, F] {
            def apply[I_, E_, O_, SE_, SO_](
                op: NoInputOp[I_, E_, O_, SE_, SO_]
            ): F[O_] = {
              val doc = testCase.params.getOrElse(Document.obj())
              buildResult match {
                case Left(onError) =>
                  onError(doc).flatMap { err =>
                    raiseError[O_](err)
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
                  .map(body =>
                    assert.bodyEql(body, actualBody, testCase.bodyMediaType)
                  )
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
  ): (Service.Reflective[NoInputOp], Request[F]) = {
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
    val request = Request[F](Method.GET, Uri.unsafeFromString("/"))
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

  def allServerTests(): List[ComplianceTest[F]] = {
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
