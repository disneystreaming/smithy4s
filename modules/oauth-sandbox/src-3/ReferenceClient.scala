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

import cats.effect._
import io.circe.Json
import org.http4s.*
import org.http4s.client.middleware.Logger
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.defaults.HttpPort
import scala.concurrent.duration.*

object ReferenceClient extends IOApp.Simple with Http4sClientDsl[IO]:

  override def run: IO[Unit] = clientResource.use(client =>
    (for
      errorOrStatusAndErrorOrJson <- client
        .run(
          POST(
            UrlForm(
              "client_id" -> expectedClientId.value,
              "client_secret" -> expectedClientSecret.value,
              "grant_type" -> "refresh_token",
              "refresh_token" -> expectedRefreshToken.value
            ),
            Uri(
              scheme = Some(Uri.Scheme.http),
              authority = Some(Uri.Authority(port = Some(HttpPort)))
            ) / "token"
          )
        )
        .use(response => response.as[Json].attempt.map(response.status -> _))
        .attempt
      _ <- errorOrStatusAndErrorOrJson match
        case Right((Status.Ok, Right(json))) =>
          for
            accessToken <- IO.fromEither(
              json.hcursor.get[String]("access_token")
            )
            _ <- IO.println(s"Access token: $accessToken")
          yield ()

        case Right((Status.BadRequest, Right(json))) =>
          for
            errorOrError <- IO
              .fromEither(
                json.hcursor.get[String]("error")
              )
              .attempt
            _ <- errorOrError match
              case Right(error) => IO.println(s"Error: $error")
              case Left(error)  => IO.println(s"Error: $error")
          yield ()

        case Right((status, Right(json))) =>
          IO.println(s"Error: $status, $json")

        case Right((status, Left(error))) =>
          IO.println(s"Error: $status, $error")

        case Left(error) =>
          IO.println(s"Error: $error")
      _ <- IO.sleep(1.second)
    yield ()).foreverM
  )

  private val clientResource = EmberClientBuilder
    .default[IO]
    .build
    .map(
      Logger.colored(
        logHeaders = true,
        logBody = true
      )
    )
