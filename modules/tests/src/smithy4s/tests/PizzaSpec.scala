/*
 *  Copyright 2021-2024 Disney Streaming
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
import smithy4s.http.HttpPayloadError
import smithy4s.example.PizzaAdminService
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpContractError
import smithy4s.http.UpstreamServiceError
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

  def menuItemWithTags(tags: List[String]) = menuItem deepMerge Json.obj(
    "tags" -> Json.arr(tags.map(Json.fromString): _*)
  )

  def menuItemWithExtraData(map: Map[String, String]) =
    menuItem deepMerge Json.obj(
      "extraData" -> Json.obj(map.map { case (k, v) =>
        k -> Json.fromString(v)
      }.toSeq: _*)
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

  routerTest("Negative: send payload with bad list items") {
    (client, uri, log) =>
      val badMenuItem = menuItemWithTags(List("a" * 11))
      for {
        res <- client.send[Json](
          POST(badMenuItem, uri / "restaurant" / "bad1" / "menu" / "item"),
          log
        )
      } yield {
        val (code, _, body) = res
        val payload = body.hcursor.downField("payload")
        val message = payload.get[String]("message")
        val path = payload.get[String]("path")
        expect(code == 400) &&
        expect(
          message == Right("length required to be >= 1 and <= 10, but was 11")
        ) &&
        expect(path == Right(".tags.0"))
      }
  }

  routerTest("Negative: send payload with a bad map key") {
    (client, uri, log) =>
      val badMenuItem = menuItemWithExtraData(Map("a" -> "foo"))
      for {
        res <- client.send[Json](
          POST(badMenuItem, uri / "restaurant" / "bad2" / "menu" / "item"),
          log
        )
      } yield {
        val (code, _, body) = res
        val payload = body.hcursor.downField("payload")
        val message = payload.get[String]("message")
        val path = payload.get[String]("path")
        expect(code == 400) &&
        expect(message == Right("length required to be >= 2, but was 1")) &&
        expect(path == Right(".extraData"))
      }
  }

  routerTest("Negative: send payload witha bad map value") {
    (client, uri, log) =>
      val badMenuItem = menuItemWithExtraData(Map("aa" -> "f" * 11))
      for {
        res <- client.send[Json](
          POST(badMenuItem, uri / "restaurant" / "bad2" / "menu" / "item"),
          log
        )
      } yield {
        val (code, _, body) = res
        val payload = body.hcursor.downField("payload")
        val message = payload.get[String]("message")
        val path = payload.get[String]("path")
        expect(code == 400) &&
        expect(
          message == Right("length required to be >= 2 and <= 10, but was 11")
        ) &&
        expect(path == Right(".extraData.0"))
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

  routerTest("Optional payload set to empty") { (client, uri, log) =>
    for {
      res <- client.send[Json](
        GET(uri / "optional-output"),
        log
      )
    } yield {
      val (code, headers, body) = res
      expect.same(body, Json.Null) &&
      expect.same(code, 200) &&
      expect(
        headers.get("Content-Length").exists(_ == List("4")),
        "Content-Length should be 4"
      ) &&
      expect.same(
        headers.get("Content-Type"),
        Some(List("application/json"))
      )
    }
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

  routerTest("Response to a HEAD request should have empty body") {
    (client, uri, log) =>
      for {
        res <- client.send[String](
          HEAD((uri / "head-request")),
          log
        )
      } yield {
        val (code, headers, body) = res
        // There may be other headers, but this one should definitely exist.
        // In general, content-length and content-type headers should be omitted
        // but we won't fail the test if they aren't since the HTTP Spec is
        // fairly vague and thus permissive in this area.
        val expectedHeaders = Map(
          "Test" -> List("test")
        )
        val containsAllExpectedHeaders =
          expectedHeaders.forall(h => headers.get(h._1).contains(h._2))
        expect.same(code, 200) &&
        expect.same(body, "") &&
        expect(
          containsAllExpectedHeaders,
          s"Expected to find all of $expectedHeaders inside of $headers"
        )
      }
  }

  routerTest("204 no content response should have empty body") {
    (client, uri, log) =>
      for {
        res <- client.send[String](
          GET((uri / "no-content")),
          log
        )
      } yield {
        val (code, _, body) = res
        expect.same(code, 204) &&
        expect.same(body, "")
      }
  }

  routerTest("Upstream service error returns 500") { (client, uri, log) =>
    val badMenuItem = Json.obj(
      "food" -> pizzaItem,
      "price" -> Json.fromFloatOrNull(9.0f)
    )

    for {
      res <- client.send[Json](
        POST(badMenuItem, uri / "restaurant" / "upstreamServiceError" / "menu" / "item"),
        log
      )
    } yield {
      val (code, headers, body) = res
      val expectedBody =
        Json.obj(
          "upstreamServiceError" -> Json.obj(
            "message" -> Json.fromString("Upstream service failure")
          )
        )
     val discriminator = headers.get("X-Error-Type").flatMap(_.headOption)

      expect(code == 500) &&
        expect(body == expectedBody) &&
        expect(discriminator == None)
    }
  }

  type Res = (Client[IO], Uri)
  def sharedResource: Resource[IO, (Client[IO], Uri)] = for {
    stateRef <- Resource.eval(
      IO.ref(PizzaAdminServiceImpl.State(Map.empty))
    )
    impl = new PizzaAdminServiceImpl(stateRef)
    res <- runServer(
      impl,
      {
        case HttpPayloadError(smithy4s.codecs.PayloadPath(List()), _, _) =>
          smithy4s.example.GenericClientError("Oops")
        case PizzaAdminServiceImpl.Boom =>
          smithy4s.example.GenericServerError("Crash")
        case UpstreamServiceError(message) =>
          UpstreamServiceError(message)
        case t: Throwable if !t.isInstanceOf[HttpContractError] =>
          // This pattern allows checking that errors specified in specs
          // do not get intercepted by mapErrors/flatMapErrors methods.
          // If it was the case, these errors would be turned into a GenericServerError
          // and would fail.
          smithy4s.example.GenericServerError("CatchAll: " + t.getMessage())
        case other => other
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
