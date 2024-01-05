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

package smithy4s.example.guides

import smithy4s.example.guides.auth._
import smithy4s.http4s._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s._
import com.comcast.ip4s._
import org.http4s.client._
import org.http4s.ember.client.EmberClientBuilder
import smithy4s.Hints
import org.http4s.headers.Authorization

object AuthClient {
  def apply(http4sClient: Client[IO]): Resource[IO, HelloWorldAuthService[IO]] =
    SimpleRestJsonBuilder(HelloWorldAuthService)
      .client(http4sClient)
      .uri(Uri.unsafeFromString("http://localhost:9000"))
      .middleware(Middleware("my-token"))
      .resource
}

object Middleware {

  private def middleware(bearerToken: String): Client[IO] => Client[IO] = {
    inputClient =>
      Client[IO] { request =>
        val newRequest = request.withHeaders(
          Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
        )

        inputClient.run(newRequest)
      }
  }

  def apply(bearerToken: String): ClientEndpointMiddleware[IO] =
    new ClientEndpointMiddleware.Simple[IO] {
      private val mid = middleware(bearerToken)
      def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): Client[IO] => Client[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            endpointHints.get[smithy.api.Auth] match {
              case Some(auths) if auths.value.isEmpty => identity
              case _                                  => mid
            }
          case None => identity
        }
      }
    }

}

object AuthClientExampleMain extends IOApp.Simple {
  val run = (for {
    client <- EmberClientBuilder.default[IO].build
    authClient <- AuthClient(client)
    health <- Resource.eval(authClient.healthCheck().flatMap(IO.println))
    hello <- Resource.eval(authClient.sayWorld().flatMap(IO.println))
  } yield ()).use_
}
