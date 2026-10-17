/*
 *  Copyright 2021-2026 Disney Streaming
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

import cats.effect.Async
import cats.implicits._
import org.http4s._
import smithy.test._
import smithy4s.Service
import smithy4s.compliancetests.TestConfig._
import smithy4s.kinds._

private[compliancetests] class MalformedRequestComplianceTestCase[
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
      testCase: HttpMalformedRequestTestCase
  ): Request[F] = {
    val req = testCase.request
    val expectedHeaders = parseHeaders(req.headers)
    val expectedMethod = Method
      .fromString(req.method)
      .getOrElse(sys.error("Invalid method"))

    val expectedUri = baseUri
      .withPath(
        Uri.Path.unsafeFromString(req.uri).addEndsWithSlash
      )
      .copy(
        query = Query.fromVector(parseQueryParams(req.queryParams))
      )
    val body =
      req.body
        .map(b => fs2.Stream.emit(b).through(fs2.text.utf8.encode))
        .getOrElse(fs2.Stream.empty)

    Request[F](
      method = expectedMethod,
      uri = expectedUri,
      headers = expectedHeaders,
      body = body
    )
  }

  private[compliancetests] def malformedRequestTest[I, E, O, SE, SO](
      endpoint: originalService.Endpoint[I, E, O, SE, SO],
      testCase: HttpMalformedRequestTestCase
  ): ComplianceTest[F] = {
    ComplianceTest[F](
      testCase.id,
      testCase.protocol,
      endpoint.id,
      testCase.documentation,
      serverMalformed,
      run = ce.defer {
        val fakeImpl: FunctorAlgebra[Alg, F] =
          originalService.fromPolyFunction[Kind1[F]#toKind5](
            new originalService.FunctorInterpreter[F] {
              def apply[I_, E_, O_, SE_, SO_](
                  op: originalService.Operation[I_, E_, O_, SE_, SO_]
              ): F[O_] = {
                raiseError(new IntendedShortCircuit)
              }
            }
          )

        routes(fakeImpl)(originalService)
          .use { server =>
            server.orNotFound
              .run(makeRequest(baseUri, testCase))
              .attempt
              .flatMap {
                case Left(_: IntendedShortCircuit) =>
                  assert
                    .fail(
                      s"Expected an error response, but the server accepted the malformed request (IntendedShortCircuit)"
                    )
                    .pure[F]
                case Left(_) =>
                  // Server threw during routing/decoding of malformed input —
                  // this is a valid rejection of the malformed request
                  assert.success.pure[F]
                case Right(resp) =>
                  resp.body
                    .through(fs2.text.utf8.decode)
                    .compile
                    .foldMonoid
                    .tupleRight(resp.status)
                    .tupleRight(resp.headers)
                    .flatMap { case ((actualBody, status), headers) =>
                      val response = testCase.response
                      val bodyAssertF: F[ComplianceTest.ComplianceResult] =
                        response.body
                          .map { malformedResponseBodyDefinition =>
                            malformedResponseBodyDefinition.assertion match {
                              case c: HttpMalformedResponseBodyAssertion.ContentsCase =>
                                assert.bodyEql(
                                  actualBody,
                                  Some(c.contents),
                                  Some(
                                    malformedResponseBodyDefinition.mediaType
                                  )
                                )
                              case c: HttpMalformedResponseBodyAssertion.MessageRegexCase =>
                                assert
                                  .regexEql(c.messageRegex, actualBody)
                                  .pure[F]
                            }
                          }
                          .getOrElse(assert.success.pure[F])
                      bodyAssertF.map { bodyAssert =>
                        bodyAssert |+|
                          assert.headersCheck(
                            headers,
                            response.headers
                          ) |+|
                          assert.eql(status.code, response.code)
                      }
                    }
              }
          }
      }
    )
  }

  private def interpolateRequest(
      request: HttpMalformedRequestDefinition,
      context: Map[String, String]
  ): HttpMalformedRequestDefinition = {
    HttpMalformedRequestDefinition(
      method = interpolateCodeTemplate(request.method, context),
      uri = interpolateCodeTemplate(request.uri, context),
      host = request.host.map(interpolateCodeTemplate(_, context)),
      queryParams =
        request.queryParams.map(_.map(interpolateCodeTemplate(_, context))),
      headers = request.headers.map(interpolateHeaderMap(_, context)),
      body = request.body.map(interpolateCodeTemplate(_, context)),
      bodyMediaType = request.bodyMediaType
    )
  }

  private def interpolateHeaderMap(
      headers: Map[String, String],
      context: Map[String, String]
  ): Map[String, String] = {
    headers
      .filterNot(_._1.equalsIgnoreCase("x-amzn-errortype"))
      .map { case (key, value) =>
        (
          interpolateCodeTemplate(key, context),
          interpolateCodeTemplate(value, context)
        )
      }
  }

  private def interpolateResponse(
      response: HttpMalformedResponseDefinition,
      context: Map[String, String]
  ): HttpMalformedResponseDefinition = {
    HttpMalformedResponseDefinition(
      code = response.code,
      headers = response.headers.map(interpolateHeaderMap(_, context)),
      body = response.body.map { body =>
        HttpMalformedResponseBodyDefinition(
          mediaType = interpolateCodeTemplate(body.mediaType, context),
          assertion = body.assertion match {
            case c: HttpMalformedResponseBodyAssertion.ContentsCase =>
              HttpMalformedResponseBodyAssertion.ContentsCase(
                interpolateCodeTemplate(c.contents, context)
              )
            case c: HttpMalformedResponseBodyAssertion.MessageRegexCase =>
              HttpMalformedResponseBodyAssertion.MessageRegexCase(
                interpolateCodeTemplate(c.messageRegex, context)
              )
          }
        )
      }
    )
  }

  private def generateMalformedRequestTests(
      testCase: HttpMalformedRequestTestCase
  ): List[HttpMalformedRequestTestCase] = {
    testCase.testParameters
      .filter(_.nonEmpty)
      .fold(List(testCase)) { params =>
        val numTests = params.values.headOption.map(_.size).getOrElse(0)
        (0 until numTests).toList.map { idx =>
          val context: Map[String, String] = params.map { case (key, values) =>
            key -> values(idx)
          }
          HttpMalformedRequestTestCase(
            id = s"${testCase.id}_$idx",
            protocol = testCase.protocol,
            request = interpolateRequest(testCase.request, context),
            response = interpolateResponse(testCase.response, context),
            documentation = testCase.documentation,
            tags = Some(
              context.values.toList.map(NonEmptyString(_))
            ),
            testParameters = testCase.testParameters
          )
        }
      }
  }

  def malformedRequestTests(): List[ComplianceTest[F]] = {
    originalService.endpoints.toList.flatMap { case endpoint =>
      endpoint.hints
        .get(HttpMalformedRequestTests)
        .map(_.value)
        .getOrElse(Nil)
        .flatMap(generateMalformedRequestTests)
        .filter(_.protocol == protocolTag.id)
        .map(tc => malformedRequestTest(endpoint, tc))
    }
  }
}
