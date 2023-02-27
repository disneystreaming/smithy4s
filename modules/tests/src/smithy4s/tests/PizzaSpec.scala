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
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Request
import org.http4s.Uri
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.typelevel.ci.CIString
import smithy4s.PayloadPath
import smithy4s.example.PizzaAdminService
import smithy4s.http.CaseInsensitive
import smithy4s.http.HttpContractError
import smithy4s.http.PayloadError
import weaver._

import java.util.UUID
import cats.Show
import org.http4s.EntityDecoder
import org.http4s.MalformedMessageBodyFailure

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

  val positiveEmptyLabel = """|Happy path:
                              |- no payload
                              |""".stripMargin.trim()

  routerTest(positiveEmptyLabel) { (client, uri, log) =>
    for {
      res <- client.send[Json](GET(uri / "version"), log)
      (code, headers, body) = res
      _ <- expect(code == 200).failFast
      version <- body.expect[String]
    } yield {
      expect(version == "version")
    }
  }

  val positiveLabel = """|Happy path:
                         |- path parameters
                         |- payload trait
                         |- custom status code
                         |- response header
                         |""".stripMargin.trim()

  routerTest(positiveLabel) { (client, uri, log) =>
    def expectedMenu(id: UUID) = Json.obj(id.toString() -> menuItem)

    val createPizza = POST(
      menuItem,
      uri / "restaurant" / "foo" / "menu" / "item"
    )

    for {
      res <- client.send[Json](createPizza, log)
      (code, headers, body) = res
      _ <- expect(code == 201).failFast
      addedAt = headers
        .get("X-ADDED-AT")
        .foldMap(identity)
      pizzaId <- body.expect[UUID]
      getMenu = GET(uri / "restaurant" / "foo" / "menu")
      res <- client.send[Json](getMenu, log)
      (code2, _, body2) = res
      _ <- expect(code2 == 200).failFast
      expected = expectedMenu(pizzaId)
    } yield {
      expect(body2 == expected) &&
      expect(addedAt.head.toDouble != 0)
    }
  }

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

  routerTest("path with special characters in parameter") {
    (client, uri, log) =>
      val createPizza = GET(
        menuItem,
        uri / "restaurant" / "foo with spaces and % percentages" / "menu"
      )

      for {
        res <- client.send[Json](createPizza, log)
        (code, headers, body) = res
      } yield {
        expect(code == 404) &&
        expect(
          body == Json.obj(
            "name" -> Json.fromString(
              "foo with spaces and % percentages"
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

  routerTest("Round trip") { (client, uri, log) =>
    for {
      res <- client.send[Json](
        POST(
          uri = (uri / "roundTrip" / "l").withQueryParam("query", "q"),
          headers = Headers(Header.Raw(CIString("HEADER"), "h")),
          body = Json.obj("body" -> Json.fromString("b"))
        ),
        log
      )
    } yield {
      val (code, headers, body) = res
      val expectedBody = Json.obj(
        "body" -> Json.fromString("b"),
        "label" -> Json.fromString("l"),
        "query" -> Json.fromString("q")
      )
      val header = headers.get("HEADER").getOrElse(Nil)
      val expectedHeader = List("h")
      expect.all(code == 200, header == expectedHeader) &&
      expect.same(body, expectedBody)
    }
  }

  private def headerTest(name: String)(requestHeaderNames: String*) =
    routerTest(name) { (client, uri, log) =>
      for {
        res <- client.send[String](
          POST.apply(
            (uri / "headers"),
            requestHeaderNames.zipWithIndex
              .map { case (name, i) =>
                name -> s"header-${i + 1}"
              }
          ),
          log
        )
      } yield {
        val (code, headers, body) = res

        expect(code == 200) &&
        expect(body.isEmpty()) &&
        expect(headers.get("x-uppercase-header") == Some(List("header-1"))) &&
        expect(headers.get("x-capitalized-header") == Some(List("header-2"))) &&
        expect(headers.get("x-lowercase-header") == Some(List("header-3"))) &&
        expect(headers.get("x-mixed-header") == Some(List("header-4")))
      }
    }

  headerTest("CI Headers - all lowercase")(
    "x-uppercase-header",
    "x-capitalized-header",
    "x-lowercase-header",
    "x-mixed-header"
  )

  headerTest("CI Headers - all uppercase")(
    "X-UPPERCASE-HEADER",
    "X-CAPITALIZED-HEADER",
    "X-LOWERCASE-HEADER",
    "X-MIXED-HEADER"
  )

  headerTest("CI Headers - all mixed")(
    "X-upPerCaSE-heADeR",
    "X-caPiTAlIZEd-HEadEr",
    "x-loweRcase-hEADeR",
    "X-mIxEd-HeAdEr"
  )

  routerTest("httpResponseCode") { (client, uri, log) =>
    for {
      res1 <- client.send[Unit](
        GET(uri = uri / "custom-code" / "201"),
        log
      )
      // on `0`, the mock returns None, so we should default to endpoint value
      res2 <- client.send[Unit](
        GET(uri = uri / "custom-code" / "0"),
        log
      )
      caught <- client
        .send[Json](
          GET(uri = uri / "custom-code" / "201"),
          log
        )
        .map(_ => false)
        .handleError { case _: MalformedMessageBodyFailure =>
          true
        }
    } yield {
      expect.same(res1._1, 201) &&
      expect.same(res2._1, 200) &&
      expect(
        caught,
        "MalformedMessageBodyFailure is expected because body should be empty"
      )
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

  routerTest("Positive: can find enum endpoint by value") {
    (client, uri, log) =>
      for {
        resValue <- client.send[Json](
          GET(uri / "get-enum" / "v1"),
          log
        )
      } yield {
        expect(resValue._1 == 200)
      }
  }

  routerTest("Positive: can find intEnum endpoint by int value value") {
    (client, uri, log) =>
      for {
        goodRes <- client.send[Json](
          GET(uri / "get-int-enum" / "1"),
          log
        )
        stringValue <- client.send[Json](
          GET(uri / "get-int-enum" / "FIRST"),
          log
        )
        badValue <- client.send[Json](
          GET(uri / "get-int-enum" / "3"),
          log
        )
      } yield {
        expect(goodRes._1 == 200) &&
        expect(goodRes._3.noSpaces == """{"result":1}""") &&
        expect(stringValue._1 == 400) &&
        expect(badValue._1 == 400)
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
        case e @ PayloadError(PayloadPath(List()), _, _) =>
          e.printStackTrace
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
