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
import org.http4s.server.middleware.Logger
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import scala.concurrent.duration.*

object SmithyServer extends IOApp.Simple:

  override def run: IO[Unit] = (for
    createAccessTokenRoute <- TokenExchangeBuilder
      .routes(TokenApiImpl)
      .resource
    _ <- EmberServerBuilder
      .default[IO]
      .withHttpApp(
        Logger.httpApp(
          logHeaders = true,
          logBody = true
        )(createAccessTokenRoute.orNotFound)
      )
      .withShutdownTimeout(0.seconds)
      .build
  yield ()).useForever

  private object TokenApiImpl extends TokenApi[IO]:
    override def createAccessToken(
        clientId: ClientId,
        clientSecret: ClientSecret,
        grantType: GrantType,
        refreshToken: RefreshToken
    ): IO[CreateAccessTokenOutput] =
      if (clientId != expectedClientId)
        IO.raiseError(BadRequest(error = Error.INVALID_CLIENT))
      else if (clientSecret != expectedClientSecret)
        IO.raiseError(BadRequest(error = Error.UNAUTHORIZED_CLIENT))
      else if (grantType != GrantType.REFRESH_TOKEN)
        IO.raiseError(BadRequest(error = Error.UNSUPPORTED_GRANT_TYPE))
      else if (refreshToken != expectedRefreshToken)
        IO.raiseError(BadRequest(error = Error.INVALID_GRANT))
      else
        IO.pure(
          CreateAccessTokenOutput(
            accessToken = AccessToken("this is an access token, promise!"),
            expiresIn = ExpiresIn(3600),
            refreshToken = expectedRefreshToken,
            tokenType = TokenType.BEARER
          )
        )
