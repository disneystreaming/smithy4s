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

package smithy4s.guides

import smithy4s.guides.auth._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.server.Middleware
import org.typelevel.ci.CIString
import scala.concurrent.duration.Duration
import smithy4s.kinds.{FunctorAlgebra, PolyFunction5, Kind1}
import smithy4s.Hints
import org.http4s.headers.Authorization
import cats.data.OptionT
import smithy4s.http4s.EndpointSpecificMiddleware

final case class APIKey(value: String)

object HelloWorldAuthImpl extends HelloWorldAuthService[IO] {
  def sayWorld(): IO[World] = World().pure[IO]
  def healthCheck(): IO[HealthCheckOutput] = HealthCheckOutput("Okay!").pure[IO]
}

trait AuthChecker {
  def isAuthorized(token: APIKey): IO[Boolean]
}

object AuthChecker extends AuthChecker {
  def isAuthorized(token: APIKey): IO[Boolean] = {
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
      .middleware(AuthMiddleware.smithy4sMiddleware(AuthChecker))
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
        .map { APIKey.apply }

      val isAuthorized = maybeKey
        .map { key =>
          authChecker.isAuthorized(key)
        }
        .getOrElse(IO.pure(false))

      isAuthorized.flatMap {
        case true => inputApp(request)
        case false =>
          IO.raiseError(new NotAuthorizedError("Not authorized!"))
      }
    }
  }

  def smithy4sMiddleware(
      authChecker: AuthChecker
  ): EndpointSpecificMiddleware.Simple[IO] =
    new EndpointSpecificMiddleware.Simple[IO] {
      def prepareUsingHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            val mid = middleware(authChecker)
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
