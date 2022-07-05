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

package smithy4s.dynamic

import smithy4s.ShapeId
import org.http4s.client.Client
import org.http4s.implicits._
import cats.implicits._

import cats.effect.IO
import cats.effect.Resource
import org.http4s.HttpApp
import smithy4s.example._
import smithy4s.http4s.SimpleRestJsonBuilder

class DynamicHttpProxy(client: Client[IO]) {

  val dynamicServiceIO =
    Utils.parseSampleSpec("pizza.smithy").map { model =>
      DynamicSchemaIndex
        .load(model)
        .getService(ShapeId("smithy4s.example", "PizzaAdminService"))
        .getOrElse(sys.error("service not found in DSI"))
    }

  val dynamicPizza: IO[smithy4s.Monadic[PizzaAdminServiceGen, IO]] =
    dynamicServiceIO
      .flatMap { dsi =>
        SimpleRestJsonBuilder(dsi.service)
          .client[IO](client, uri"http://localhost:8080")
          .liftTo[IO]
          .map { dynamicClient =>
            JsonIOProtocol
              .fromJsonIO[PizzaAdminServiceGen, PizzaAdminServiceOperation](
                JsonIOProtocol.toJsonIO(dynamicClient)(dsi.service)
              )

          }
      }
}

object Http4sDynamicPizzaClientSpec extends smithy4s.tests.PizzaClientSpec {

  def makeClient = Left { (httpApp: HttpApp[IO]) =>
    Resource.eval(
      new DynamicHttpProxy(Client.fromHttpApp(httpApp)).dynamicPizza
    )
  }

}
