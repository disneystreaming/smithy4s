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
import smithy.test._
import smithy4s.Service
import smithy4s.kinds._
import software.amazon.smithy.utils._


private[compliancetests] class MalformedRequestComplianceTestCase[
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
      .withMultiValueQueryParams(
        parseQueryParams(req.queryParams)
      )
    val body =
      req.body
        .map(b => fs2.Stream.emit(b).through(ce.utf8Encode))
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
      endpoint.id,
      TestConfig.clientReq,
      run = {
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
              .attemptNarrow[IntendedShortCircuit]
              .flatMap {
                case Left(_) =>
                  assert
                    .fail(
                      s"Expected a Error Response, but got a IntendedShortCircuit error"
                    )
                    .pure[F]
                case Right(resp) =>
                  resp.body
                    .through(utf8Decode)
                    .compile
                    .foldMonoid
                    .tupleRight(resp.status)
                    .tupleRight(resp.headers)
                    .map {
                      case ((actualBody, status), headers) => {
                        val response = testCase.response
                        val bodyAssert = response.body
                          .map(malformedResponseBodyDefinition => {
                             malformedResponseBodyDefinition.assertion match {
                              case HttpMalformedResponseBodyAssertion
                                    .ContentsCase(contents) =>    assert.bodyEql(contents, actualBody, Some(malformedResponseBodyDefinition.mediaType))
                              case HttpMalformedResponseBodyAssertion
                                    .MessageRegexCase(messageRegex) => assert.regexEql(messageRegex, actualBody)
                            }

                          })
                        val assertions =
                          bodyAssert.toList :+
                            assert.headersCheck(headers, response.headers) :+
                            assert.eql(status.code, response.code)
                        assertions.combineAll
                      }
                    }
              }
          }
      }
    )

  }

  /**
    * From the docs:
    *  The lists of values for each key must be identical in length. One test permutation is generated for each index the parameter lists.
    * For example, parameters with 5 values for each key will generate 5 tests in total.
    */

  private def formatRequest(request:HttpMalformedRequestDefinition, arg:String):HttpMalformedRequestDefinition ={
     val codeWriter = new SimpleCodeWriter()
    import codeWriter._
    val formatOnly:String => String = str =>if(str.contains("$value")) format(str, arg) else str
    println(arg)
    println(request)

    val formattedMethod = formatOnly(request.method)
    val formattedUri = formatOnly(request.uri)
    val formattedHost = request.host.map(formatOnly(_))
    val formattedQueryParams = request.queryParams.map(_.map(formatOnly(_)))
    val formattedHeaders = request.headers.map(_.map{ case (key, value) => (formatOnly(key), formatOnly(value))})

    HttpMalformedRequestDefinition(
      method = formattedMethod,
      uri = formattedUri,
      host = formattedHost,
      queryParams = formattedQueryParams,
      headers = formattedHeaders,
      body = request.body
      )
  }
  private def generateMalformedRequestTests(malformedRequestTestCase: HttpMalformedRequestTestCase):List[HttpMalformedRequestTestCase] = {
    println(malformedRequestTestCase.testParameters)
    malformedRequestTestCase.testParameters.flatMap(_.get("value")).fold(List(malformedRequestTestCase)) {
      value =>
        value.map(v => malformedRequestTestCase.copy(
          request = formatRequest(malformedRequestTestCase.request, v)
        ))
    }
  }

  def malformedRequestTests(): List[ComplianceTest[F]] = {
    originalService.endpoints.flatMap { case endpoint =>
      endpoint.hints
        .get(HttpMalformedRequestTests)
        .map(_.value)
        .getOrElse(Nil)
        .flatMap(generateMalformedRequestTests)
       //  .filter(_.protocol == protocolTag.id.toString())
        .map(tc => malformedRequestTest(endpoint, tc))

    }
  }
}
