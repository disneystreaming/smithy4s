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

import smithy4s.guides.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.server.Middleware

object HelloWorldImpl extends HelloWorldService[IO] {
  def sayWorld(): IO[World] = World().pure[IO]
}

object Routes {
  import org.http4s.server.middleware._

  val customMiddleware = CORS.policy.withAllowOriginAll
    .withAllowCredentials(false)
    .apply(_: HttpRoutes[IO])

  private val helloRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  val all: Resource[IO, HttpRoutes[IO]] =
    helloRoutes.map(r => customMiddleware(r))
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    EmberServerBuilder
      .default[IO]
      .withPort(port"9000")
      .withHost(host"localhost")
      .withHttpApp(routes.orNotFound)
      .build
  }.useForever

}
