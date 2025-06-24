package smithy4s.http4s

import smithy4s.example.RoutingService
import cats.effect._
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits._

object Http4sRoutingSpec extends smithy4s.tests.RoutingSpec {
  def runServer(
      routingService: RoutingService[IO],
      errorTransformation: PartialFunction[Throwable, Throwable]
  ): Resource[IO, Res] = {
    SimpleRestJsonBuilder
      .routes(routingService)
      .mapErrors(errorTransformation)
      .resource
      .map { httpRoutes =>
        val client = Client.fromHttpApp(httpRoutes.orNotFound)
        val uri = Uri.unsafeFromString("http://localhost")
        (client, uri)
      }
  }
}
