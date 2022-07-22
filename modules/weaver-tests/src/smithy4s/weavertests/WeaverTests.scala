package smithy4s.weavertests
import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.implicits._
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Uri
import org.http4s.headers.`Content-Type`
import smithy.test._
import smithy4s.Document
import smithy4s.Endpoint
import smithy4s.Service
import smithy4s.UnsupportedProtocolError
import smithy4s.http.PayloadError
import weaver._

import java.nio.charset.StandardCharsets

import concurrent.duration._
import cats.kernel.Eq

abstract class WeaverTests[
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    client: (
        HttpApp[IO],
        Uri
    ) => Either[UnsupportedProtocolError, smithy4s.Monadic[Alg, IO]],
    server: smithy4s.Monadic[Alg, IO] => Either[
      UnsupportedProtocolError,
      HttpRoutes[IO]
    ]
)(implicit service: Service[Alg, Op])
    extends SimpleIOSuite {
  import org.http4s.implicits._

  private val baseUri = uri"http://localhost"
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
      body = fs2.Stream.emit(expectedBody).through(fs2.text.utf8.encode[IO])
    )
  }

  private def matchRequest(
      request: Request[IO],
      expected: Request[IO]
  ) = request.bodyText.compile.string.flatMap { requestBody =>
    expected.bodyText.compile.string.map { expectedBody =>
      assert.eql(request.uri, expected.uri) &&
      assert.eql(request.method, expected.method) &&
      assert.eql(request.headers.removePayloadHeaders, expected.headers) &&
      assert.eql(requestBody, expectedBody)
    }
  }

  private def handle[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO]
  ) = {

    val requestTests =
      endpoint.hints.get(HttpRequestTests).map(_.value).getOrElse(Nil)

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)

    requestTests.flatMap { testCase =>
      List(
        makeClientTest(endpoint, testCase, inputFromDocument),
        makeServerTest(endpoint, testCase, inputFromDocument)
      )
    }
  }

  private def makeServerTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ) =
    // todo: protocol check
    test(endpoint.id.toString + "(server): " + testCase.id) {
      Deferred[IO, I]
        .flatMap { requestDeferred =>
          type R[I_, E_, O_, SE_, SO_] = IO[O_]

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

          server(fakeImpl)
            .liftTo[IO]
            .flatMap(_.orNotFound.run(makeRequest(baseUri, testCase)))
            .attemptNarrow[NotImplementedError] *>
            requestDeferred.get.flatMap { foundInput =>
              inputFromDocument
                .decode(testCase.params.getOrElse(sys.error("no params: todo")))
                .liftTo[IO]
                .map { decodedInput =>
                  // todo: derive Eq from schemas?
                  implicit val eqq: Eq[I] = Eq.fromUniversalEquals
                  assert.eql(foundInput, decodedInput)
                }
            }
        }
    }

  private def makeClientTest[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO],
      testCase: HttpRequestTestCase,
      inputFromDocument: Document.Decoder[I]
  ) = {
    // todo: protocol check
    test(endpoint.id.toString + "(client): " + testCase.id) {

      type R[I_, E_, O_, SE_, SO_] = IO[O_]

      Deferred[IO, Request[IO]]
        .flatMap { requestDeferred =>
          val theClient: IO[smithy4s.Monadic[Alg, IO]] = client(
            HttpApp[IO] { req =>
              // Save consumed stream for later reuse
              req.body.compile.toVector
                .map(fs2.Stream.emits(_))
                .map(req.withBodyStream(_))
                .flatMap(requestDeferred.complete(_))
                .as(Response[IO]())
            },
            baseUri
          ).liftTo[IO]

          theClient
            .flatMap { client =>
              inputFromDocument
                .decode(
                  testCase.params.getOrElse(sys.error("no params, todo"))
                )
                .liftTo[IO]
                .flatMap { in =>
                  {
                    service
                      .asTransformation[R](
                        client
                      )
                      .apply(endpoint.wrap(in)): IO[O]
                  }
                    // We don't expect the response to parse, as it's empty.
                    .attemptNarrow[PayloadError]
                }
            } *> requestDeferred.get
            // should be complete by now, but just to avoid blocking the test forever...
            .timeout(1.second)
        }
        .flatMap(matchRequest(_, makeRequest(baseUri, testCase)))
    }
  }

  // todo use stream-based way of defining tests
  service.service.endpoints
    .flatMap {
      handle(_)
    }
    .foreach(identity(_))

}
