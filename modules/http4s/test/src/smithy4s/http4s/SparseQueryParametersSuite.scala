package smithy4s.http4s

import cats.effect.kernel.Deferred
import cats.effect.IO
import io.circe.Json
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.client.Client
import org.http4s.implicits._

import smithy4s.example.{ServiceWithSparseQueryParams, SparseQueryOutput}
import weaver._

object SparseQueryParametersSuite extends SimpleIOSuite with CirceInstances {
  
  test("Server handles sparse query parameters") {
    runServerTest().map { response =>
      assert.same(
        Json.obj("foo" -> Json.arr(Json.fromString("bar"), Json.Null, Json.fromString("baz"))),
        response
      )
    }
  }

  test("Client handles sparse query parameters") {
    runClientTest(List(Some("bar"), None, Some("baz"))).map { request =>
      assert.same(
        Map("foo" -> List(Some("bar"), None, Some("baz"))),
        request.query
      )
    }
  }

  private def runServerTest(): IO[Json] = {
    def run(
        routes: HttpRoutes[IO],
        req: Request[IO]
    ): IO[Json] =
      routes.orNotFound.run(req).flatMap { response =>
        response.as[Json]
      }
    SimpleRestJsonBuilder
      .routes(Impl)
      .resource
      .use { routes =>
        for {
          result <- run(
            routes,
            Request[IO](method = Method.GET, uri = uri"/operation/sparse-query-params?foo=bar&foo&foo=baz")
          )
        } yield result
      }
  }

  private def runClientTest(
      input: List[Option[String]]
  ): IO[TestRequest] = {
    val resources = for {
      promise <- Deferred[IO, Request[IO]].toResource
      reqBody = Json.obj("foo" -> Json.arr(input.map(_.fold(Json.Null)(Json.fromString)): _*)).toString().getBytes()
      httpClient: Client[IO] = Client(req =>
        req
          .toStrict(None)
          .flatMap(promise.complete)
          .as(Response[IO](body = fs2.Stream.emits(reqBody)))
          .toResource
      )
      client <- SimpleRestJsonBuilder
        .apply(ServiceWithSparseQueryParams)
        .client(httpClient)
        .resource
    } yield (promise, client)
    resources.use { case (promise, client) =>
      client.getOperation(input) >> promise.get.flatMap { req =>
        IO(TestRequest(queryParamsToMap(req.uri.query)))
      }
    }
  }

  def queryParamsToMap(query: Query): Map[String, List[Option[String]]] = {
    query.pairs.groupBy(_._1).map { case (k, v) =>
      k -> v.map(_._2).toList
    }
  }

  case class TestResponse(body: Json)

  case class TestRequest(query: Map[String, List[Option[String]]])

  object Impl extends ServiceWithSparseQueryParams[IO] {
    override def getOperation(foo: List[Option[String]]): IO[SparseQueryOutput] = {
      IO.pure(SparseQueryOutput(foo))
    }
  }

}
