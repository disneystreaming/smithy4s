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
import cats.effect.Async
import cats.effect.syntax.all._
import cats.kernel.Eq
import org.http4s._
import org.http4s.headers.`Content-Type`
import smithy.test._
import smithy4s.{Document, Errorable, Hints, Service, ShapeId}
import smithy4s.kinds._

import scala.concurrent.duration._
import smithy4s.compliancetests.internals.eq.EqSchemaVisitor
import smithy4s.compliancetests.TestConfig._
import cats.MonadThrow
import java.util.concurrent.TimeoutException
private[compliancetests] class ServerHttpComplianceTestCase[
    F[_],
    Alg[_[_, _, _, _, _]]
](
    router: Router[F],
    serviceInstance: Service[Alg]
)(implicit
    ce: Async[F]
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
      testCase.headers.foldMap[Headers](headers => Headers(headers.toList))

    val expectedContentType = testCase.bodyMediaType
      .foldMap(mt => Headers(`Content-Type`(MediaType.unsafeParse(mt))))

    val allExpectedHeaders = expectedHeaders ++ expectedContentType

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
        .map(b => fs2.Stream.emit(b).through(fs2.text.utf8.encode))
        .getOrElse(fs2.Stream.empty)

    Request[F](
      method = expectedMethod,
      uri = expectedUri,
      headers = allExpectedHeaders,
      body = body
    )
  }

  private[compliancetests] def serverRequestTest[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[F] = {
    implicit val inputEq: Eq[I] = EqSchemaVisitor(endpoint.input)
    val testModel = CanonicalSmithyDecoder
      .fromSchema(endpoint.input)
      .decode(testCase.params.getOrElse(Document.obj()))
      .liftTo[F]
    ComplianceTest[F](
      testCase.id,
      testCase.protocol,
      endpoint.id,
      testCase.documentation,
      serverReq,
      run = ce.defer {
        deferred[I].flatMap { inputDeferred =>
          val fakeImpl: FunctorAlgebra[Alg, F] =
            originalService.fromPolyFunction[Kind1[F]#toKind5](
              new originalService.FunctorInterpreter[F] {
                def apply[I_, E_, O_, SE_, SO_](
                    op: originalService.Operation[I_, E_, O_, SE_, SO_]
                ): F[O_] = {
                  val endpointInternal = originalService.endpoint(op)
                  val in = originalService.input(op)
                  if (endpointInternal.id == endpoint.id)
                    inputDeferred.complete(in.asInstanceOf[I]) *>
                      raiseError(new IntendedShortCircuit)
                  else raiseError(new Throwable("Wrong endpoint called"))
                }
              }
            )

          object ServerError {
            def unapply(response: Response[F]): Boolean =
              response.status.responseClass match {
                case Status.ServerError => true
                case _                  => false
              }
          }

          routes(fakeImpl)(originalService)
            .use { server =>
              server.orNotFound
                .run(makeRequest(baseUri, testCase))
                .attempt
                .flatMap {
                  case Left(_: IntendedShortCircuit) | Right(ServerError()) =>
                    inputDeferred.get
                      .timeout(1.second)
                      .flatMap { foundInput =>
                        testModel
                          .map { decodedInput =>
                            assert.eql(foundInput, decodedInput)
                          }
                      }
                      .recover { case _: TimeoutException =>
                        val message =
                          """|Timed-out while waiting for an input.
                             |
                             |This probably means that the Router implementation either failed to decode the request
                             |or failed to route the decoded input to the correct service method.
                             |""".stripMargin
                        assert.fail(message)
                      }
                  case Left(error) => MonadThrow[F].raiseError(error)
                  case Right(response) =>
                    response.as[String].map { message =>
                      assert.fail(
                        s"Expected either an IntendedShortCircuit error or a 5xx response, but got a response with status ${response.status} and message ${message}"
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
      testCase.id,
      testCase.protocol,
      endpoint.id,
      testCase.documentation,
      serverRes,
      run = ce.defer {
        val (amendedService, syntheticRequest) = prepareService(endpoint)

        val buildResult: Either[Document => F[Throwable], Document => F[O]] = {
          errorSchema
            .toLeft {
              val outputDecoder: Document.Decoder[O] =
                CanonicalSmithyDecoder.fromSchema(endpoint.output)
              (doc: Document) =>
                outputDecoder
                  .decode(doc)
                  .liftTo[F]
            }
            .left
            .map(_.kleisliFy[F])
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

        routes(fakeImpl)(amendedService)
          .use { server =>
            server.orNotFound
              .run(syntheticRequest)
              .flatMap { resp =>
                resp.body
                  .through(fs2.text.utf8.decode)
                  .compile
                  .foldMonoid
                  .tupleRight(resp.status)
                  .tupleRight(resp.headers)
              }
              .flatMap { case ((actualBody, status), headers) =>
                assert
                  .bodyEql(actualBody, testCase.body, testCase.bodyMediaType)
                  .map { bodyAssert =>
                    bodyAssert |+|
                      assert.testCase.checkHeaders(testCase, headers) |+|
                      assert.eql(status.code, testCase.code)
                  }
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
        override def endpoints: Vector[Endpoint[_, _, _, _, _]] = Vector(amendedEndpoint)
        override def input[I_, E_, O_, SI_, SO_](op: NoInputOp[I_, E_, O_, SI_, SO_]) : I_ = ???
        override def ordinal[I_, E_, O_, SI_, SO_](op: NoInputOp[I_, E_, O_, SI_, SO_]): Int = ???
        override def version: String = originalService.version
        override def hints: Hints = originalService.hints
      }
      // format: on
    (amendedService, request)
  }

  def allServerTests(): List[ComplianceTest[F]] = {
    def toResponse[I, E, O, SE, SO](
        endpoint: originalService.Endpoint[I, E, O, SE, SO]
    ) = {
      endpoint.errorable.toList
        .flatMap { errorable =>
          errorable.error.alternatives.flatMap { errorAlt =>
            errorAlt.schema.hints
              .get(HttpResponseTests)
              .toList
              .flatMap(_.value)
              .filter(_.protocol == protocolTag.id.toString())
              .filter(tc => tc.appliesTo.forall(_ == AppliesTo.SERVER))
              .map(tc =>
                serverResponseTest(
                  endpoint,
                  tc,
                  errorSchema = Some(
                    ErrorResponseTest
                      .from(
                        errorAlt,
                        errorable
                      )
                  )
                )
              )
          }
        }
    }
    originalService.endpoints.toList.flatMap { case endpoint =>
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

      val errorResponseTests = toResponse(endpoint)
      requestsTests ++ opResponseTests ++ errorResponseTests
    }
  }
}
