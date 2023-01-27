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
import org.http4s.HttpApp
import org.http4s.client.Client
import smithy4s.ShapeId
import smithy4s.dynamic._
import smithy4s.example._
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.{Model => SModel}

class DynamicHttpProxy(client: Client[IO]) {

  object JsonIOProtocol extends smithy4s.tests.JsonProtocolF[IO]

  val dynamicServiceIO =
    parseSampleSpec("pizza.smithy")
      .flatMap { model =>
        DynamicSchemaIndex
          .loadModel(model)
          .liftTo[IO]
      }
      .map { index =>
        index
          .getService(ShapeId("smithy4s.example", "PizzaAdminService"))
          .getOrElse(sys.error("service not found in DSI"))
      }

  val dynamicPizza: IO[PizzaAdminService[IO]] =
    dynamicServiceIO
      .flatMap { dsi =>
        SimpleRestJsonBuilder(dsi.service)
          .client[IO](client)
          .use
          .liftTo[IO]
          .map { dynamicClient =>
            JsonIOProtocol
              .fromJsonF[PizzaAdminServiceGen, PizzaAdminServiceOperation](
                JsonIOProtocol.toJsonF(dynamicClient)(dsi.service)
              )

          }
      }

  private def parseSampleSpec(fileName: String): IO[SModel] =
    IO(
      SModel
        .assembler()
        .discoverModels()
        .putProperty(ModelAssembler.DISABLE_JAR_CACHE, true)
        .addImport(s"./sampleSpecs/$fileName")
        .assemble()
        .unwrap()
    )
}

object Http4sDynamicPizzaClientSpec extends smithy4s.tests.PizzaClientSpec {

  def makeClient = Left { (httpApp: HttpApp[IO]) =>
    Resource.eval(
      new DynamicHttpProxy(Client.fromHttpApp(httpApp)).dynamicPizza
    )
  }

  def server(app: HttpApp[IO]): Resource[IO, Int] =
    Resource.eval(IO.raiseError(new Exception("this shouldn't be used")))
}
