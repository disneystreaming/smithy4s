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

package smithy4s
package sandbox
package oauth

import io.circe.Json
import org.http4s.*
import org.http4s.server.middleware.Logger
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.ember.server.EmberServerBuilder
import zio.*
import zio.interop.catz.*

object ReferenceServer extends ZIOAppDefault with Http4sDsl[Task]:

  override def run: RIO[Scope, Unit] = for
    _ <- EmberServerBuilder
      .default[Task]
      .withHttpApp(
        Logger.httpApp(
          logHeaders = true,
          logBody = true
        )(createAccessTokenRoute.orNotFound)
      )
      .withShutdownTimeout(0.seconds.asScala)
      .build
      .toScopedZIO
    _ <- ZIO.never
  yield ()

  private val createAccessTokenRoute: HttpRoutes[Task] = HttpRoutes.of {
    case request @ POST -> Root / "token" =>
      request.decodeStrict[UrlForm](parameters =>
        ZIO.succeed {
          def equal(key: String, value: String): Boolean =
            parameters.getFirst(key).contains(value)
          def badRequest(error: String): Response[Task] =
            Response(BadRequest).withEntity(
              Json.obj(
                "error" -> Json.fromString(error)
              )
            )
          if (!equal("client_id", expectedClientId.value))
            badRequest("invalid_client")
          else if (!equal("client_secret", expectedClientSecret.value))
            badRequest("unauthorized_client")
          else if (!equal("grant_type", GrantType.REFRESH_TOKEN.value))
            badRequest("unsupport_grant_type")
          else if (!equal("refresh_token", expectedRefreshToken.value))
            badRequest("invalid_grant")
          else
            Response(Ok).withEntity(
              Json.obj(
                "access_token" -> Json.fromString(
                  "this is an access token, promise!"
                ),
                "token_type" -> Json.fromString("Bearer"),
                "expires_in" -> Json.fromLong(3600),
                "refresh_token" -> Json.fromString(expectedRefreshToken.value)
              )
            )
        }
      )

  }
