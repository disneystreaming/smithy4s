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

import org.http4s.server.middleware.Logger
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import zio.*
import zio.interop.catz.*

object SmithyServer extends ZIOAppDefault:

  override def run: RIO[Scope, Unit] = for
    createAccessTokenRoute <- ZIO.fromEither(
      TokenExchangeBuilder
        .routes(TokenApiImpl)
        .make
    )
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

  private object TokenApiImpl extends TokenApi[Task]:
    override def createAccessToken(
        clientId: ClientId,
        clientSecret: ClientSecret,
        grantType: GrantType,
        refreshToken: RefreshToken
    ): Task[CreateAccessTokenOutput] =
      if (clientId != expectedClientId)
        ZIO.fail(BadRequest(error = Error.INVALID_CLIENT))
      else if (clientSecret != expectedClientSecret)
        ZIO.fail(BadRequest(error = Error.UNAUTHORIZED_CLIENT))
      else if (grantType != GrantType.REFRESH_TOKEN)
        ZIO.fail(BadRequest(error = Error.UNSUPPORTED_GRANT_TYPE))
      else if (refreshToken != expectedRefreshToken)
        ZIO.fail(BadRequest(error = Error.INVALID_GRANT))
      else
        ZIO.succeed(
          CreateAccessTokenOutput(
            accessToken = AccessToken("this is an access token, promise!"),
            expiresIn = ExpiresIn(3600),
            refreshToken = expectedRefreshToken,
            tokenType = TokenType.BEARER
          )
        )
