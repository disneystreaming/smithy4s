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

import cats.data.Chain
import cats.effect._
import cats.effect.std.UUIDGen
import cats.Show
import cats.syntax.all._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.HttpApp
import org.typelevel.ci.CIString
import smithy4s.example._
import smithy4s.Timestamp
import weaver._

abstract class PizzaClientSpec extends IOSuite {

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

  clientTest("Errors make it through") { (client, backend, log) =>
    for {
      ts <- IO(Timestamp(Timestamp.nowUTC().epochSecond, 0))
      uuid <- UUIDGen.randomUUID[IO]
      response <- Created(
        Json.fromString(uuid.toString),
        Header.Raw(
          CIString("X-ADDED-AT"),
          ts.epochSecond.toString()
        )
      )
      _ <- backend.prepResponse("foo", response)
      menuItem = MenuItem(
        Food.PizzaCase(
          Pizza("margharita", PizzaBase.TOMATO, List(Ingredient.Mushroom))
        ),
        price = 9.5f
      )
      result <- client.addMenuItem("foo", menuItem)
      request <- backend.lastRequest("foo")
      requestBody <- request.asJson
    } yield {

      val pizzaItem = Json.obj(
        "pizza" -> Json.obj(
          "name" -> Json.fromString("margharita"),
          "base" -> Json.fromString("T"),
          "toppings" -> Json.arr(
            Json.fromString("Mushroom")
          )
        )
      )

      val expectedBody = Json.obj(
        "food" -> pizzaItem,
        "price" -> Json.fromFloatOrNull(9.5f)
      )

      val expectedResult = AddMenuItemResult(uuid.toString(), ts)

      expect(requestBody == expectedBody) &&
      expect(request.uri.path.toString == "/restaurant/foo/menu/item") &&
      expect(result == expectedResult)
    }
  }

  clientTestForError(
    "Receives errors as exceptions",
    Response(status = Status.NotFound)
      .withHeaders(Header.Raw(CIString("X-Error-Type"), "NotFoundError"))
      .withEntity(
        Json.obj("name" -> Json.fromString("bar"))
      ),
    NotFoundError("bar")
  )

  clientTestForError(
    "Handle error with a unique status code mapping (418)",
    Response(status = Status.ImATeapot)
      .withEntity(
        Json.obj("message" -> Json.fromString("generic error message for 418"))
      ),
    GenericClientError("generic error message for 418")
  )

  clientTestForError(
    "Handle error w/o a discriminator header nor a unique status code",
    Response(status = Status.ProxyAuthenticationRequired)
      .withEntity(
        Json.obj("message" -> Json.fromString("generic client error message"))
      ),
    smithy4s.http.UnknownErrorResponse(
      407,
      Map("Content-Length" -> "42", "Content-Type" -> "application/json"),
      """{"message":"generic client error message"}"""
    )
  )

  clientTest("Headers are case insensitive") { (client, backend, log) =>
    for {
      res <- client.headerEndpoint(
        uppercaseHeader = "upper".some,
        capitalizedHeader = "capitalized".some,
        lowercaseHeader = "lowercase".some,
        mixedHeader = "mixed".some
      )
    } yield {
      expect(res.uppercaseHeader == "upper".some) &&
      expect(res.capitalizedHeader == "capitalized".some) &&
      expect(res.lowercaseHeader == "lowercase".some) &&
      expect(res.mixedHeader == "mixed".some)
    }
  }

  clientTest("code decoded in structure") { (client, backend, log) =>
    val code = 201
    for {
      response <- Created()
      _ <- backend.prepResponse(s"customCode$code", response)
      res <- client.customCode(code)
    } yield {
      expect(res.code == Some(201))
    }
  }

  clientTest("Round trip") { (client, backend, log) =>
    for {
      res <- client.roundTrip(
        label = "l",
        query = "q".some,
        header = "h".some,
        body = "b".some
      )
    } yield {
      val expected = RoundTripData(
        "l",
        header = Some("h"),
        query = Some("q"),
        body = Some("b")
      )
      expect.same(res, expected)
    }
  }

  def clientTestForError[E](
      name: String,
      response: Response[IO],
      expected: E
  )(implicit
      loc: SourceLocation,
      ct: scala.reflect.ClassTag[E],
      show: Show[E] = Show.fromToString[E]
  ) = {
    clientTest(name) { (client, backend, log) =>
      for {
        _ <- backend.prepResponse(
          name,
          response
        )
        maybeResult <- client.getMenu(name).attempt
      } yield maybeResult match {
        case Right(_) => failure("expected failure")
        case Left(error: E) =>
          expect.same[E](error, expected)
        case Left(error) =>
          failure(s"Error of unexpected type: $error")

      }
    }
  }

  def clientTest(name: TestName)(
      f: (
          PizzaAdminService[IO],
          Backend,
          Log[IO]
      ) => IO[Expectations]
  ): Unit =
    test(name) { (res, log) => f(res._1, res._2, log) }

  // If right, TCP will be exercised.
  def makeClient: Either[
    HttpApp[IO] => Resource[IO, PizzaAdminService[IO]],
    Int => Resource[IO, PizzaAdminService[IO]]
  ]

  type Res = (PizzaAdminService[IO], Backend)
  def sharedResource: Resource[IO, (PizzaAdminService[IO], Backend)] =
    for {
      ref <- Resource.eval(IO.ref(State.empty))
      app = router(ref)
      client <- makeClient match {
        case Left(fromHttpApp) => fromHttpApp(app)
        case Right(fromPort) =>
          for {
            port <- retryResource(server(app))
            client <- fromPort(port)
          } yield client
      }
    } yield (client, Backend(ref))

  case class Backend(ref: Ref[IO, State]) {
    def prepResponse(key: String, response: Response[IO]): IO[Unit] =
      ref.update(_.prepResponse(key, response))

    def lastRequest(key: String): IO[Request[IO]] =
      ref.get.flatMap(_.lastRequest(key))
  }

  case class State(
      requests: Map[String, Chain[Request[IO]]],
      nextResponses: Map[String, Response[IO]]
  ) {
    def lastRequest(key: String): IO[Request[IO]] = requests
      .get(key)
      .flatMap(_.lastOption)
      .liftTo[IO](new Throwable(s"Found no request matching $key"))

    def saveRequest(key: String, request: Request[IO]) = {
      val reqForKey = requests.getOrElse(key, Chain.empty)
      val updated = reqForKey.append(request)
      this.copy(requests = requests + (key -> updated))
    }

    def prepResponse(key: String, response: Response[IO]) =
      this.copy(nextResponses = nextResponses + (key -> response))

    def getResponse(key: String): IO[Response[IO]] = nextResponses
      .get(key)
      .liftTo[IO](new Throwable(s"Found no response matching $key"))
  }

  object State {
    val empty = State(Map.empty, Map.empty)
  }

  def router(ref: Ref[IO, State]) = {
    def storeAndReturn(key: String, request: Request[IO]): IO[Response[IO]] =
      // Collecting the whole body eagerly to make sure we don't consume it after closing the connection
      request.body.compile.toVector.flatMap { body =>
        ref
          .updateAndGet(
            _.saveRequest(key, request.withBodyStream(fs2.Stream.emits(body)))
          )
          .flatMap(_.getResponse(key))
      }

    object Q extends OptionalQueryParamDecoderMatcher[String]("query")

    HttpRoutes
      .of[IO] {
        case request @ (POST -> Root / "restaurant" / key / "menu" / "item") =>
          storeAndReturn(key, request)
        case request @ (GET -> Root / "restaurant" / key / "menu") =>
          storeAndReturn(key, request)
        case request @ POST -> Root / "headers" =>
          Ok().map(_.withHeaders(request.headers))
        case request @ POST -> Root / "roundTrip" / label :? Q(q) =>
          val headers = request.headers
            .get(CIString("HEADER"))
            .map(Headers(_))
            .getOrElse(Headers.empty)

          request.asJson
            .map(
              _.deepMerge(
                Json
                  .obj(
                    "label" -> Json.fromString(label),
                    "query" -> q.map(Json.fromString).getOrElse(Json.Null)
                  )
                  .deepDropNullValues
              )
            )
            .flatMap(json => Ok(json, headers = headers))
        case request @ (GET -> Root / "custom-code" / IntVar(code)) =>
          storeAndReturn(s"customCode$code", request)

        case POST -> Root / "book" / _ =>
          val body = Json.obj("message" -> Json.fromString("test"))
          Ok(body)
      }
      .orNotFound
  }

  val randomInt = Resource.eval(IO(scala.util.Random.nextInt(9999)))

  val randomPort = randomInt.map(_ + 50000)

  def server(app: HttpApp[IO]): Resource[IO, Int]

  def retryResource[A](
      resource: Resource[IO, A],
      max: Int = 10
  ): Resource[IO, A] =
    if (max <= 0) resource
    else resource.orElse(retryResource(resource, max - 1))

}
