package smithy4s.aws

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.HttpApp
import smithy4s.kinds.FunctorAlgebra
import smithy4s.{Service, ShapeTag}
import smithy4s.compliancetests._
import smithy4s.http.HttpMediaType
import smithy4s.schema.Schema

object AwsJson {
  def impl[A](
      protocol: smithy4s.ShapeTag.Companion[A]
  ): ReverseRouter[IO] = new ReverseRouter[IO] {
    type Protocol = A
    val protocolTag: ShapeTag[A] = protocol.getTag

    def expectedResponseType(schema: Schema[_]): HttpMediaType = HttpMediaType(
      "application/json"
    )

    def reverseRoutes[Alg[_[_, _, _, _, _]]](
        app: HttpApp[IO],
        testHost: Option[String] = None
    )(implicit service: Service[Alg]): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      AwsEnvironment
        .default(Client.fromHttpApp(app), AwsRegion.US_EAST_1)
        .flatMap(env => AwsClient(service, env))
    }
  }

}
