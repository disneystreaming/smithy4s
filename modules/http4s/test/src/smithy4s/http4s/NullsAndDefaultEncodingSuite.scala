package smithy4s.http4s

import weaver._
import org.http4s._
import org.http4s.implicits._
import cats.effect.IO
import smithy4s.example.ServiceWithNullsAndDefaults
import smithy4s.example.OperationOutput
import io.circe.Json
import org.typelevel.ci.CIString
import org.typelevel.ci._
import org.http4s.circe.CirceInstances
import org.http4s.client.Client
import smithy4s.example.OperationInput
import cats.effect.kernel.Deferred

object NullsAndDefaultEncodingSuite extends SimpleIOSuite with CirceInstances {

  test("routes - explicit defaults encoding = false") {
    runServerTest(explicitDefaults = false).map { response =>
      assert.same(
        Map(ci"required-header-with-default" -> "required-header-with-default"),
        response.headers
      ) &&
      assert.same(
        Json.obj(
          "requiredWithDefault" -> Json.fromString("required-default")
        ),
        response.body
      )
    }
  }

  test("routes - explicit defaults encoding = true") {
    runServerTest(explicitDefaults = true).map { response =>
      assert.same(
        Map(ci"required-header-with-default" -> "required-header-with-default"),
        response.headers
      ) &&
      assert.same(
        Json.obj(
          "requiredWithDefault" -> Json.fromString("required-default"),
          "optionalWithDefault" -> Json.fromString("optional-default"),
          "optional" -> Json.Null
        ),
        response.body
      )
    }
  }

  test("client - explicit defaults encoding = false") {
    runClientTest(explicitDefaults = false, OperationInput())
      .map { request =>
        assert.same(
          Map(
            ci"required-header-with-default" -> "required-header-with-default"
          ),
          request.headers
        ) &&
        assert.same(
          Map.empty,
          request.query
        ) &&
        assert.same(
          List("operation", "required-label-with-default"),
          request.labels
        ) &&
        assert.same(
          Json.obj(
            "requiredWithDefault" -> Json.fromString("required-default")
          ),
          request.body
        )
      }
  }

  test("client - explicit defaults encoding = true") {
    runClientTest(explicitDefaults = true, OperationInput())
      .map { request =>
        assert.same(
          Map(
            ci"optional-header-with-default" -> "optional-header-with-default",
            ci"required-header-with-default" -> "required-header-with-default"
          ),
          request.headers
        ) && assert.same(
          Map(
            "optional-query-with-default" -> "optional-query-with-default",
            "required-query-with-default" -> "required-query-with-default"
          ),
          request.query
        ) && assert.same(
          List("operation", "required-label-with-default"),
          request.labels
        ) && assert.same(
          Json.obj(
            "optional" -> Json.Null,
            "optionalWithDefault" -> Json.fromString("optional-default"),
            "requiredWithDefault" -> Json.fromString("required-default")
          ),
          request.body
        )
      }
  }

  object Impl extends ServiceWithNullsAndDefaults[IO] {
    override def operation(input: OperationInput): IO[OperationOutput] =
      IO.pure(OperationOutput())
  }

  private val specHeaders = Set(
    ci"optional-header",
    ci"optional-header-with-default",
    ci"required-header-with-default"
  )

  case class TestResponse(headers: Map[CIString, String], body: Json)

  case class TestRequest(
      headers: Map[CIString, String],
      query: Map[String, String],
      labels: List[String],
      body: Json
  )

  private def runServerTest(explicitDefaults: Boolean): IO[TestResponse] = {
    def run(
        routes: HttpRoutes[IO],
        req: Request[IO]
    ): IO[(Map[CIString, String], Json)] =
      routes.orNotFound.run(req).flatMap { response =>
        response.as[Json].map(headersToMap(response.headers) -> _)
      }
    SimpleRestJsonBuilder
      .withExplicitDefaultsEncoding(explicitDefaults)
      .routes(Impl)
      .resource
      .use { routes =>
        for {
          result <- run(
            routes,
            Request[IO](method = Method.POST, uri = uri"/operation/label")
          )
          (headers, body) = result
        } yield TestResponse(headers, body)
      }
  }

  private def runClientTest(
      explicitDefaults: Boolean,
      input: OperationInput
  ): IO[TestRequest] = {
    val resources = for {
      promise <- Deferred[IO, Request[IO]].toResource
      httpClient: Client[IO] = Client(req =>
        req
          .toStrict(None)
          .flatMap(promise.complete)
          .as(Response[IO]())
          .toResource
      )
      client <- SimpleRestJsonBuilder
        .withExplicitDefaultsEncoding(explicitDefaults)
        .apply(ServiceWithNullsAndDefaults)
        .client(httpClient)
        .resource
    } yield (promise, client)
    resources.use { case (promise, client) =>
      client.operation(input) >> promise.get.flatMap { req =>
        val labels = req.uri.path.segments
          .map(_.toString)
          .toList
        req
          .as[Json]
          .map(body =>
            TestRequest(
              headersToMap(req.headers),
              queryToMap(req.uri.query),
              labels,
              body
            )
          )
      }
    }
  }

  private def headersToMap(headers: Headers) = headers.headers.flatMap { h =>
    if (specHeaders.contains(h.name)) Some(h.name -> h.value) else None
  }.toMap

  private def queryToMap(query: Query) =
    query.pairs.flatMap(kv => kv._2.map(kv._1 -> _)).toMap
}
