package smithy4s.aws

import cats.effect.{IO, Resource}
import org.http4s.client.Client
import org.http4s.HttpApp
import smithy4s.kinds.FunctorAlgebra
import smithy4s.{Service, ShapeTag}
import smithy4s.aws.http4s._
import smithy4s.compliancetests._

object AwsJson {
  def impl[A](
      protocol: smithy4s.ShapeTag.Companion[A],
      codecApi: smithy4s.http.CodecAPI
  ): ReverseRouter[IO] = new ReverseRouter[IO] {
    type Protocol = A
    val protocolTag: ShapeTag[A] = protocol.getTag

    def codecs: smithy4s.http.CodecAPI = codecApi

    def reverseRoutes[Alg[_[_, _, _, _, _]]](app: HttpApp[IO])(implicit
        service: Service[Alg]
    ): Resource[IO, FunctorAlgebra[Alg, IO]] = {
      service.simpleAwsClient(
        Client.fromHttpApp(app),
        smithy4s.aws.kernel.AwsRegion.AP_EAST_1
      )
    }
  }

}
