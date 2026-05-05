/*
 *  Copyright 2021-2026 Disney Streaming
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

package smithy4s.http4s

import cats.effect.IO
import cats.syntax.all._
import io.circe.Json
import org.http4s._
import org.http4s.circe.CirceInstances
import org.http4s.implicits._
import smithy4s.example.UnpackedItem
import smithy4s.example.UnpackedOutputService
import weaver._

object UnpackedOutputSpec extends SimpleIOSuite with CirceInstances {

  private object Impl extends UnpackedOutputService[IO] {
    override def getRequiredItem(): IO[UnpackedItem] =
      IO.pure(UnpackedItem("required-id"))
    override def getOptionalItem(): IO[Option[UnpackedItem]] =
      IO.pure(Some(UnpackedItem("optional-id")))
    override def getPayloadItem(): IO[UnpackedItem] =
      IO.pure(UnpackedItem("payload-id"))
    override def getHeaderItem(): IO[String] =
      IO.pure("header-id")
    override def getStatusCode(): IO[Int] =
      IO.pure(201)
  }

  private val routesResource =
    SimpleRestJsonBuilder.routes(Impl).resource

  test("server: required field is wire-encoded as the wrapping struct") {
    routesResource.use { routes =>
      routes.orNotFound
        .run(Request[IO](method = Method.GET, uri = uri"/required"))
        .flatMap(_.as[Json])
        .map { body =>
          expect.same(
            Json
              .obj("item" -> Json.obj("id" -> Json.fromString("required-id"))),
            body
          )
        }
    }
  }

  test("server: optional field is wire-encoded as the wrapping struct") {
    routesResource.use { routes =>
      routes.orNotFound
        .run(Request[IO](method = Method.GET, uri = uri"/optional"))
        .flatMap(_.as[Json])
        .map { body =>
          expect.same(
            Json
              .obj("item" -> Json.obj("id" -> Json.fromString("optional-id"))),
            body
          )
        }
    }
  }

  test("server: @httpPayload field is wire-encoded as the bare payload") {
    routesResource.use { routes =>
      routes.orNotFound
        .run(Request[IO](method = Method.GET, uri = uri"/payload"))
        .flatMap(resp => resp.as[Json].tupleLeft(resp.status))
        .map { case (status, body) =>
          expect.same(Status.Ok, status) &&
            expect.same(Json.obj("id" -> Json.fromString("payload-id")), body)
        }
    }
  }

  test("server: @httpHeader field is wire-encoded in a response header") {
    routesResource.use { routes =>
      routes.orNotFound
        .run(Request[IO](method = Method.GET, uri = uri"/header"))
        .flatMap(resp =>
          resp.as[String].map { body =>
            val headerValue =
              resp.headers
                .get(org.typelevel.ci.CIString("X-Item-Id"))
                .map(_.head.value)
            expect.same(Status.Ok, resp.status) &&
            expect.same(Some("header-id"), headerValue) &&
            expect.same("{}", body)
          }
        )
    }
  }

  test("server: @httpResponseCode field is wire-encoded as the status code") {
    routesResource.use { routes =>
      routes.orNotFound
        .run(Request[IO](method = Method.GET, uri = uri"/status"))
        .map { resp =>
          expect.same(Status.Created, resp.status)
        }
    }
  }

  test("client/server roundtrip preserves the unpacked output values") {
    routesResource.use { routes =>
      val client = org.http4s.client.Client.fromHttpApp(routes.orNotFound)
      SimpleRestJsonBuilder(UnpackedOutputService)
        .client(client)
        .resource
        .use { service =>
          for {
            required <- service.getRequiredItem()
            optional <- service.getOptionalItem()
            payload <- service.getPayloadItem()
            header <- service.getHeaderItem()
            status <- service.getStatusCode()
          } yield expect.same(UnpackedItem("required-id"), required) &&
            expect.same(Some(UnpackedItem("optional-id")), optional) &&
            expect.same(UnpackedItem("payload-id"), payload) &&
            expect.same("header-id", header) &&
            expect.same(201, status)
        }
    }
  }

}
