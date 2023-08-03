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
import org.http4s.client.middleware.Logger
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.defaults.HttpPort
import zio.*
import zio.interop.catz.*

object ReferenceClient extends ZIOAppDefault with Http4sClientDsl[Task]:

  override def run: RIO[Scope, Unit] = for
    client <- EmberClientBuilder
      .default[Task]
      .build
      .map(
        Logger.colored(
          logHeaders = true,
          logBody = true
        )
      )
      .toScopedZIO
    oauthSandboxApiUri = Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(Uri.Authority(port = Some(HttpPort)))
    )
    _ <- (for
      statusAndJson <- client
        .run(
          POST(
            UrlForm(
              "client_id" -> expectedClientId.value,
              "client_secret" -> expectedClientSecret.value,
              "grant_type" -> "refresh_token",
              "refresh_token" -> expectedRefreshToken.value
            ),
            oauthSandboxApiUri / "token"
          )
        )
        .use(response => response.as[Json].map(response.status -> _))
      _ <- statusAndJson match
        case (Status.Ok, json) =>
          for
            accessToken <- ZIO.fromEither(
              json.hcursor.get[String]("access_token")
            )
            _ <- Console.printLine(s"Access token: $accessToken")
          yield ()

        case (Status.BadRequest, json) =>
          for
            error <- ZIO.fromEither(
              json.hcursor.get[String]("error")
            )
            _ <- Console.printLine(s"Error: $error")
          yield ()

        case (status, json) =>
          Console.printLine(s"Error: $status, $json")
    yield ()).repeat(Schedule.spaced(1.second))
  yield ()
