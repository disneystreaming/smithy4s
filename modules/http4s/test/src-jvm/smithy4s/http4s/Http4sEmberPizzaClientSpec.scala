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

package smithy4s.http4s

import cats.effect.IO
import cats.effect.Resource
import cats.implicits._
import com.comcast.ip4s.Port
import com.comcast.ip4s._
import org.http4s.HttpApp
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.example.PizzaAdminService
import smithy4s.example._
import weaver._

object Http4sEmberPizzaClientSpec extends IOSuite {
  type Res = PizzaAdminService[IO]

  override def sharedResource: Resource[IO, Res] = {
    SimpleRestJsonBuilder
      .routes(dummyImpl)
      .resource
      .flatMap(r => retryResource(server(r.orNotFound)))
      .flatMap { port => makeClient(port) }
  }

  def makeClient(port: Int): Resource[IO, PizzaAdminService[IO]] =
    EmberClientBuilder.default[IO].build.flatMap { client =>
      SimpleRestJsonBuilder(PizzaAdminService)
        .client(client)
        .uri(Uri.unsafeFromString(s"http://localhost:$port"))
        .resource
    }

  def server(app: HttpApp[IO]): Resource[IO, Int] =
    cats.effect.std.Random
      .scalaUtilRandom[IO]
      .flatMap(_.betweenInt(50000, 60000))
      .toResource
      .flatMap(port =>
        Port
          .fromInt(port)
          .toRight(new Exception(s"Invalid port: $port"))
          .liftTo[IO]
          .toResource
      )
      .flatMap { port =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"localhost")
          .withPort(port)
          .withHttpApp(app)
          .build
          .map(_ => port.value)
      }

  test("empty body") { client =>
    (client.reservation("name") *> client.reservation("name2")).as(success)
  }

  private val dummyImpl: PizzaAdminService[IO] =
    new PizzaAdminService.Default[IO](IO.stub) {
    // format: off
    override def health(query: Option[String]): IO[HealthResponse] = IO.pure(HealthResponse("good"))
    override def roundTrip(label: String, header: Option[String], query: Option[String], body: Option[String]): IO[RoundTripData] = IO.stub
    override def reservation(name: String, town: Option[String]): IO[ReservationOutput] = IO.pure(ReservationOutput("name"))
    // format: on
    }

  def retryResource[A](
      resource: Resource[IO, A],
      max: Int = 10
  ): Resource[IO, A] =
    if (max <= 0) resource
    else resource.orElse(retryResource(resource, max - 1))
}
