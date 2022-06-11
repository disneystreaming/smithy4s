package smithy4s.weavertests
import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.implicits._
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpApp
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

abstract class WeaverTests[
    Alg[_[_, _, _, _, _]],
    Op[_, _, _, _, _]
](
    client: (
        HttpApp[IO],
        Uri
    ) => Either[UnsupportedProtocolError, smithy4s.Monadic[Alg, IO]]
)(implicit service: Service[Alg, Op])
    extends SimpleIOSuite {
  import org.http4s.implicits._

  private val baseUri = uri"http://localhost"

  private def matchRequest(
      request: Request[IO],
      testCase: HttpRequestTestCase
  ) = request.bodyText.compile.string.map { requestBody =>
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

    assert.eql(request.uri, expectedUri) &&
    assert.eql(request.method, expectedMethod) &&
    assert.eql(request.headers.removePayloadHeaders, expectedHeaders) &&
    assert.eql(requestBody, expectedBody)
  }

  private def handle[I, E, O, SE, SO](
      endpoint: Endpoint[Op, I, E, O, SE, SO]
  ) = {

    val requestTests =
      endpoint.hints.get(HttpRequestTests).map(_.value).getOrElse(Nil)

    val inputFromDocument = Document.Decoder.fromSchema(endpoint.input)

    requestTests.map { testCase =>
      // todo: protocol check
      test(endpoint.name + ": " + testCase.id) {

        Deferred[IO, Request[IO]]
          .flatMap { requestDeferred =>
            val theClient: IO[smithy4s.Monadic[Alg, IO]] = client(
              {

                HttpApp[IO] { req =>
                  // Save consumed stream for later reuse
                  req.body.compile.toVector
                    .map(fs2.Stream.emits(_))
                    .map(req.withBodyStream(_))
                    .flatMap(requestDeferred.complete(_))
                    .as(Response[IO]())
                }
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
                    service
                      .asTransformation(client)
                      .apply(endpoint.wrap(in))
                      // We don't expect the response to parse, as it's empty.
                      .attemptNarrow[PayloadError]
                  }
              } *> requestDeferred.get
              // should be complete by now, but just to avoid blocking the test forever...
              .timeout(1.second)
          }
          .flatMap(matchRequest(_, testCase))
      }
    }
  }

  // todo use stream-based way of defining tests
  service.service.endpoints
    .flatMap {
      handle(_)
    }
    .foreach(identity(_))

}
