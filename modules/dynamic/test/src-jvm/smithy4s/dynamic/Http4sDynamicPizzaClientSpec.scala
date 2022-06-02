package smithy4s.dynamic

import smithy4s.api.SimpleRestJson
import smithy4s.SchemaIndex
import smithy4s.ShapeId
import org.http4s.client.Client
import org.http4s.implicits._
import cats.implicits._

import cats.effect.IO
import cats.effect.Resource
import org.http4s.HttpApp
import smithy4s.example._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy.api

class DynamicHttpProxy(client: Client[IO]) {

  val dynamicServiceIO =
    Utils.parseSampleSpec("pizza.smithy").map { model =>
      DynamicSchemaIndex
        .load(
          model,
          SimpleRestJson.protocol.schemas ++
            SchemaIndex(SimpleRestJson, api.Error)
        )
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
