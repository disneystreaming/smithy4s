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
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import smithy.test._
import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.http.CodecAPI
import smithy4s.http.internals.PathEncode
import smithy4s.http.internals.SchemaVisitorPathEncoder
import smithy4s.http.Metadata
import smithy4s.http4s.EntityCompiler
import smithy4s.Service
import smithy4s.ShapeTag
import smithy4s.tests.DefaultSchemaVisitor

import scala.concurrent.duration._

abstract class ServerHttpComplianceTestCase[
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

  def getServer(impl: smithy4s.Monadic[Alg, IO]): Resource[IO, HttpRoutes[IO]]
  def codecs: CodecAPI
  private val ec = EntityCompiler.fromCodecAPI[IO](codecs)
  private val ecCache = ec.createCache()

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
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase
  ): ComplianceTest[IO] = {
    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)
    ComplianceTest[IO](
      name = endpoint.id.toString + "(server|request): " + testCase.id,
      run = {
        deferred[I].flatMap { inputDeferred =>
          val fakeImpl: smithy4s.Monadic[Alg, IO] =
            service.transform[R](
              new smithy4s.Interpreter[Op, IO] {
                def apply[I_, E_, O_, SE_, SO_](
                    op: Op[I_, E_, O_, SE_, SO_]
                ): IO[O_] = {
                  val (in, endpointInternal) = service.endpoint(op)

                  if (endpointInternal.id == endpoint.id)
                    inputDeferred.complete(in.asInstanceOf[I]) *>
                      IO.raiseError(new NotImplementedError)
                  else IO.raiseError(new Throwable("Wrong endpoint called"))
                }
              }
            )

          getServer(fakeImpl)
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
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpResponseTestCase
  ): ComplianceTest[IO] = {
    def makeRequest(
        input: I,
        httpTrait: smithy.api.Http,
        pathEncode: PathEncode[I]
    ): Request[IO] = {
      val method = Method
        .fromString(httpTrait.method.value)
        .getOrElse(sys.error("Invalid method"))
      val metadata = Metadata.Encoder.fromSchema(endpoint.input).encode(input)
      val inputHasBody =
        Metadata.TotalDecoder.fromSchema(endpoint.input).isEmpty
      val path = pathEncode.encode(input)
      val uri = baseUri
        .copy(path = baseUri.path.addSegments(path.map(Uri.Path.Segment(_))))
        .withMultiValueQueryParams(metadata.query)
      val headers = Headers(metadata.headers.flatMap { case (k, v) =>
        v.map(Header.Raw(CIString(k.toString), _))
      }.toList)
      val baseRequest = Request[IO](method, uri, headers = headers)
      implicit val encoder: EntityEncoder[IO, I] = ec
        .compileEntityEncoder(endpoint.input, ecCache)
      if (inputHasBody) {
        baseRequest.withEntity(input)
      } else baseRequest
    }
    type R[I_, E_, O_, SE_, SO_] = IO[O_]

    ComplianceTest[IO](
      name = endpoint.id.toString + "(server|response): " + testCase.id,
      run = {
        val input = DefaultSchemaVisitor(endpoint.input)
        val outputDecoder = Document.Decoder.fromSchema(endpoint.output)
        val output = outputDecoder
          .decode(testCase.params.getOrElse(Document.obj()))
          .liftTo[IO]

        val requestUtils = endpoint.hints
          .get[smithy.api.Http]
          .toRight(new Exception("@http trait required to build request"))
          .flatMap(http =>
            SchemaVisitorPathEncoder(endpoint.input.addHints(http))
              .toRight(new Exception("PathEncode required to build request"))
              .tupleLeft(http)
          )
          .liftTo[IO]

        (output, requestUtils).tupled.flatMap {
          case (output, (httpTrait, pathEncode)) =>
            val fakeImpl: smithy4s.Monadic[Alg, IO] =
              service.transform[R](
                new smithy4s.Interpreter[Op, IO] {
                  def apply[I_, E_, O_, SE_, SO_](
                      op: Op[I_, E_, O_, SE_, SO_]
                  ): IO[O_] = {
                    // todo error structures
                    IO.pure(output.asInstanceOf[O_])
                  }
                }
              )

            getServer(fakeImpl)
              .use { server =>
                server.orNotFound
                  .run(makeRequest(input, httpTrait, pathEncode))
                  .flatMap { resp =>
                    resp.body
                      .through(utf8Decode)
                      .compile
                      .foldMonoid
                      .tupleRight(resp.status)
                  }
                  .map { case (actualBody, status) =>
                    val bodyAssert = testCase.body
                      .map(body => assert.eql(body, actualBody))
                    val assertions =
                      bodyAssert.toList :+
                        assert.eql(status.code, testCase.code)
                    assertions.combineAll
                  }
              }
        }
      }
    )
  }

  def allServerTests(): List[ComplianceTest[IO]] = {
    service.endpoints.flatMap { case endpoint =>
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

      requestsTests ++ opResponseTests
    }
  }
}
