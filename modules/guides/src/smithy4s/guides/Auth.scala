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
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.Hints
import org.http4s.headers.Authorization
import smithy4s.http4s.ServerEndpointMiddleware

final case class ApiToken(value: String)

object HelloWorldAuthImpl extends HelloWorldAuthService[IO] {
  def sayWorld(): IO[World] = World().pure[IO]
  def healthCheck(): IO[HealthCheckOutput] = HealthCheckOutput("Okay!").pure[IO]
}

trait AuthChecker {
  def isAuthorized(token: ApiToken): IO[Boolean]
}

object AuthChecker extends AuthChecker {
  def isAuthorized(token: ApiToken): IO[Boolean] = {
    IO.pure(
      token.value.nonEmpty
    ) // put your logic here, currently just makes sure the token is not empty
  }
}

object AuthExampleRoutes {
  import org.http4s.server.middleware._

  private val helloRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder
      .routes(HelloWorldAuthImpl)
      .middleware(AuthMiddleware(AuthChecker))
      .resource

  val all: Resource[IO, HttpRoutes[IO]] =
    helloRoutes
}

object AuthMiddleware {

  private def middleware(
      authChecker: AuthChecker
  ): HttpApp[IO] => HttpApp[IO] = { inputApp =>
    HttpApp[IO] { request =>
      val maybeKey = request.headers
        .get[`Authorization`]
        .collect {
          case Authorization(
                Credentials.Token(AuthScheme.Bearer, value)
              ) =>
            value
        }
        .map { ApiToken.apply }

      val isAuthorized = maybeKey
        .map { key =>
          authChecker.isAuthorized(key)
        }
        .getOrElse(IO.pure(false))

      isAuthorized.ifM(
        ifTrue = inputApp(request),
        ifFalse = IO.raiseError(new NotAuthorizedError("Not authorized!"))
      )
    }
  }

  def apply(
      authChecker: AuthChecker
  ): ServerEndpointMiddleware[IO] =
    new ServerEndpointMiddleware.Simple[IO] {
      private val mid: HttpApp[IO] => HttpApp[IO] = middleware(authChecker)
      def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[IO] => HttpApp[IO] = {
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

// test with `curl localhost:9000/hello -H 'Authorization: Bearer Some'`
// or `curl localhost:9000/hello`
object AuthExampleMain extends IOApp.Simple {
  val run = (for {
    routes <- AuthExampleRoutes.all
    server <- EmberServerBuilder
      .default[IO]
      .withPort(port"9000")
      .withHost(host"localhost")
      .withHttpApp(routes.orNotFound)
      .build
  } yield server).useForever
}
