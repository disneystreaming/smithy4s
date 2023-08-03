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

import org.http4s.Uri
import org.http4s.client.middleware.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.server.defaults.HttpPort
import zio.*
import zio.interop.catz.*

object SmithyClient extends ZIOAppDefault:

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
    oauthSandboxTokenApi <- TokenExchangeBuilder(TokenApi)
      .client(client)
      .uri(oauthSandboxApiUri)
      .resource
      .toScopedZIO
    _ <- (for
      createAccessTokenResult <- oauthSandboxTokenApi
        .createAccessToken(
          clientId = expectedClientId,
          clientSecret = expectedClientSecret,
          grantType = GrantType.REFRESH_TOKEN,
          refreshToken = expectedRefreshToken
        )
        .either
      _ <- createAccessTokenResult match
        case Right(createAccessTokenOutput) =>
          Console.printLine(
            s"Access token: ${createAccessTokenOutput.accessToken}"
          )

        case Left(badRequest: BadRequest) =>
          Console.printLine(s"Error: ${badRequest.error}")

        case Left(error) =>
          Console.printLine(s"Error: $error")
    yield ()).repeat(Schedule.spaced(1.second))
  yield ()
