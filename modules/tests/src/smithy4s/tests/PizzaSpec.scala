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

package smithy4s.tests

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import io.circe._
import org.http4s.Request
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import smithy4s.PayloadPath
import smithy4s.example.PizzaAdminService
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpContractError
import smithy4s.http.PayloadError
import weaver._
import cats.Show
import org.http4s.EntityDecoder

abstract class PizzaSpec
    extends IOSuite
    with Http4sDsl[IO]
    with Http4sClientDsl[IO] {

  def runServer(
      pizzaService: PizzaAdminService[IO],
      errorAdapter: PartialFunction[Throwable, Throwable]
  ): Resource[IO, Res]

  val pizzaItem = Json.obj(
    "pizza" -> Json.obj(
      "name" -> Json.fromString("margharita"),
      "base" -> Json.fromString("T"),
      "toppings" -> Json.arr(
        Json.fromString("Mushroom"),
        Json.fromString("Tomato")
      )
    )
  )

  val menuItem = Json.obj(
    "food" -> pizzaItem,
    "price" -> Json.fromFloatOrNull(9.0f)
  )

  routerTest("path that returns NotFound") { case (client, uri, log) =>
    val getPizza = GET(
      menuItem,
      uri / "restaurant" / "unknown" / "menu"
    )

    for {
      res <- client.send[Json](getPizza, log)
      (code, headers, body) = res
    } yield {
      expect(code == 404) &&
      expect(headers.get("X-Error-Type") == Some(List("NotFoundError"))) &&
      expect(
        body == Json.obj(
          "name" -> Json.fromString(
            "unknown"
          )
        )
      )
    }
  }

  val customErrorLabel = """|Negative:
                            |- custom error
                            |- default client error code
                            |- header discriminator
                            |- document response body
                            |""".stripMargin
  routerTest(customErrorLabel) { (client, uri, log) =>
    val badMenuItem =
      Json.obj(
        "food" -> pizzaItem,
        "price" -> Json.fromFloatOrNull(1.5f)
      )

    for {
      res <- client.send[Json](
        POST(badMenuItem, uri / "restaurant" / "bad1" / "menu" / "item"),
        log
      )
    } yield {
      val (code, headers, body) = res
      val expectedBody =
        Json.obj(
          "message" -> Json.fromString("Prices must be whole numbers: 1.5")
        )
      val discriminator = headers.get("X-Error-Type").flatMap(_.headOption)

      expect(headers.get("X-CODE") == Some(List("1"))) &&
      expect(code == 400) &&
      expect(body == expectedBody) &&
      expect(discriminator == Some("PriceError"))
    }
  }

  routerTest("Negative: default parsing error") { (client, uri, log) =>
    val badSalad = Json.obj(
      "salad" -> Json.obj(
        "name" -> Json.fromString("margharita"),
        "toppings" -> Json.arr(
          Json.fromString("Mushroom"),
          Json.fromString("Tomato")
        )
      )
    )

    val badMenuItem =
      Json.obj(
        "food" -> badSalad,
        "price" -> Json.fromFloatOrNull(1f)
      )

    for {
      res <- client.send[Json](
        POST(
          badMenuItem,
          uri / "restaurant" / "foo" / "menu" / "item"
        ),
        log
      )
    } yield {
      val (code, headers, body) = res
      val discriminator = headers.get("X-Error-Type")

      val payload = body.hcursor.downField("payload")
      val path = payload.get[String]("path")
      val message = payload.get[String]("message")
      expect(code == 400) &&
      expect(path.contains(".food.salad.ingredients")) &&
      expect(discriminator.isEmpty) &&
      expect(
        message.exists(_.contains("Missing required field"))
      )
    }
  }

  routerTest("Negative: top level missing required") { (client, uri, log) =>
    val badMenuItem = Json.obj()

    for {
      res <- client.send[Json](
        POST(
          badMenuItem,
          uri / "restaurant" / "foo" / "menu" / "item"
        ),
        log
      )
    } yield {
      val (code, headers, body) = res
      val discriminator = headers.get("X-Error-Type")

      val payload = body.hcursor.downField("payload")
      val path = payload.get[String]("path")
      val message = payload.get[String]("message")
      expect(code == 400) &&
      expect(path.contains(".food")) &&
      expect(discriminator.isEmpty) &&
      expect(
        message.exists(_.contains("Missing required field"))
      )
    }
  }

  routerTest("Negative: error transformation") { (client, uri, log) =>
    for {
      res <- client.send[Json](
        POST(
          menuItem,
          uri / "restaurant" / "boom" / "menu" / "item"
        ),
        log
      )
    } yield {
      val (code, headers, body) = res
      val discriminator = headers.get("X-Error-Type").flatMap(_.headOption)
      val message = body.hcursor.downField("message").as[String]
      expect(code == 502) &&
      expect(discriminator == Some("GenericServerError")) &&
      expect(
        message.exists(_.contains("Crash"))
      )
    }
  }

  routerTest("Negative: client error transformation") { (client, uri, log) =>
    val addBadSalad = POST(
      Json.Null,
      uri / "restaurant" / "foo" / "menu" / "item"
    )

    for {
      res <- client.send[Json](addBadSalad, log)
    } yield {
      val (code, headers, body) = res
      val discriminator = headers.get("X-Error-Type").flatMap(_.headOption)
      val message = body.hcursor.downField("message").as[String]
      expect(code == 418) &&
      expect(discriminator == Some("GenericClientError")) &&
      expect(
        message.exists(_.contains("Oops"))
      )
    }
  }

  routerTest("Negative: / doesn't match") { (client, uri, log) =>
    for {
      status <- client.status(GET(uri))
    } yield {
      expect(status.code == 404)
    }
  }

  routerTest("Health check") { (client, uri, log) =>
    for {
      res <- client.send[Json](GET(uri / "health"), log)
    } yield {
      val (code, _, _) = res
      expect(code == 200)
    }
  }

  routerTest("Health check - fail length constraint on query") {
    (client, uri, log) =>
      for {
        res <- client.send[Json](
          GET((uri / "health").withQueryParam("query", "1" * 6)),
          log
        )
      } yield {
        val (code, _, _) = res
        expect(code == 400)
      }
  }

  routerTest("path param failing refinement results in a BadRequest") {
    (client, uri, log) =>
      client
        .send[Unit](
          POST(uri = uri / "echo" / "too-short").withEntity(Json.obj()),
          log
        )
        .map(_._1)
        .map(assert.eql(_, 400))
  }

  routerTest("query param failing refinement results in a BadRequest") {
    (client, uri, log) =>
      client
        .send[Unit](
          POST(
            (uri / "echo" / "long-enough")
              .withQueryParam("queryParam", "too-short")
          ).withEntity(Json.obj()),
          log
        )
        .map(_._1)
        .map(assert.eql(_, 400))
  }

  routerTest("body failing refinement results in a BadRequest") {
    (client, uri, log) =>
      client
        .send[Unit](
          POST(
            uri / "echo" / "long-enough"
          ).withEntity(Json.obj("data" -> Json.fromString("too-short"))),
          log
        )
        .map(_._1)
        .map(assert.eql(_, 400))
  }

  // note: these aren't really part of the pizza suite

  pureTest("Happy path: httpMatch") {
    val matchResult = smithy4s.http
      .httpMatch(
        PizzaAdminService.service,
        smithy4s.http.HttpMethod.POST,
        Vector("restaurant", "foo", "menu", "item")
      )
      .map { case (endpoint, _, map) =>
        endpoint.name -> map
      }
    expect(
      matchResult == Some(
        ("AddMenuItem", Map("restaurant" -> "foo"))
      )
    )
  }

  routerTest(
    "HEAD request should have empty body - content length from just brackets"
  ) { (client, uri, log) =>
    for {
      res <- client.send[String](
        HEAD((uri / "head-request")),
        log
      )
    } yield {
      val (code, headers, body) = res
      val expectedHeaders = Map(
        "Test" -> List("test"),
        // response would have been `{}` if this was a GET
        "Content-Length" -> List("2"),
        "Content-Type" -> List("application/json")
      )
      val containsProperHeaders =
        expectedHeaders.forall(h => headers.get(h._1).isDefined)
      expect.same(code, 200) &&
      expect.same(body, "") &&
      expect(
        containsProperHeaders,
        s"Expected to find all of $expectedHeaders inside of $headers"
      )
    }
  }

  routerTest("HEAD request should have empty body - longer content length") {
    (client, uri, log) =>
      for {
        res <- client.send[String](
          HEAD((uri / "head-request").withQueryParam("test", "one")),
          log
        )
      } yield {
        val (code, headers, body) = res
        val expectedHeaders = Map(
          "Test" -> List("test"),
          // response would have been `{"bodyField":"one"}` if this was a GET
          "Content-Length" -> List("19"),
          "Content-Type" -> List("application/json")
        )
        val containsProperHeaders =
          expectedHeaders.forall(h => headers.get(h._1).contains(h._2))
        expect.same(code, 200) &&
        expect.same(body, "") &&
        expect(
          containsProperHeaders,
          s"Expected to find all of $expectedHeaders inside of $headers"
        )
      }
  }

  pureTest("Negative: http no match (bad path)") {
    val matchResult = smithy4s.http.httpMatch(
      PizzaAdminService.service,
      smithy4s.http.HttpMethod.POST,
      Vector("restaurants", "foo", "menu", "item")
    )
    expect(matchResult == None)
  }

  pureTest("Negative: http no match (bad method)") {
    val matchResult = smithy4s.http.httpMatch(
      PizzaAdminService.service,
      smithy4s.http.HttpMethod.PATCH,
      Vector("restaurant", "foo", "menu", "item")
    )
    expect(matchResult == None)
  }

  type Res = (Client[IO], Uri)
  def sharedResource: Resource[IO, (Client[IO], Uri)] = for {
    stateRef <- Resource.eval(
      Compat.ref(PizzaAdminServiceImpl.State(Map.empty))
    )
    impl = new PizzaAdminServiceImpl(stateRef)
    res <- runServer(
      impl,
      {
        case PayloadError(PayloadPath(List()), _, _) =>
          smithy4s.example.GenericClientError("Oops")
        case PizzaAdminServiceImpl.Boom =>
          smithy4s.example.GenericServerError("Crash")
        case t: Throwable if !t.isInstanceOf[HttpContractError] =>
          // This pattern allows checking that errors specified in specs
          // do not get intercepted by mapErrors/flatMapErrors methods.
          // If it was the case, these errors would be turned into a GenericServerError
          // and would fail.
          smithy4s.example.GenericServerError("CatchAll: " + t.getMessage())
      }
    )
  } yield res

  def routerTest(testName: TestName)(
      f: (Client[IO], Uri, Log[IO]) => IO[Expectations]
  ) = test(testName)((res: Res, log: Log[IO]) => f(res._1, res._2, log))

  implicit class ClientOps(client: Client[IO]) {
    // Returns: (status, headers, body)
    def send[A: Show](
        request: Request[IO],
        log: Log[IO]
    )(implicit A: EntityDecoder[IO, A]): IO[(Int, HeaderMap, A)] =
      client.run(request).use { response =>
        val code = response.status.code
        val headers =
          HeaderMap {
            response.headers.headers
              .groupBy(ci => CaseInsensitive(ci.name.toString))
              .map { case (k, v) =>
                k -> v.map(_.value)
              }
          }
        val payloadIO = response.as[A]
        log.info("code = " + code) *>
          log.info("headers = " + headers) *>
          payloadIO.flatTap(p => log.info("payload = " + p.show)).map {
            payload => (code, headers, payload)
          }
      }

  }

  case class HeaderMap(
      private val values: Map[CaseInsensitive, List[String]]
  ) {
    def get(key: String): Option[List[String]] =
      values.get(CaseInsensitive(key))
  }

  implicit class JsonOps(json: Json) {
    def expect[A: Decoder](implicit loc: SourceLocation): IO[A] =
      json.as[A] match {
        case Left(value) =>
          IO.raiseError(AssertionException(value.message, NonEmptyList.of(loc)))
        case Right(value) => IO.pure(value)
      }
  }

}
