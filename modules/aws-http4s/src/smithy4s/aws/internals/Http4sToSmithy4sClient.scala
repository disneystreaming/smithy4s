package smithy4s.aws.internals

import org.http4s.client.Client
import org.http4s.{Request, Response}
import smithy4s.client.UnaryLowLevelClient
import cats.effect.MonadCancelThrow

private[aws] object Http4sToSmithy4sClient {
  def apply[F[_]: MonadCancelThrow](
      client: Client[F]
  ): UnaryLowLevelClient[F, Request[F], Response[F]] =
    new UnaryLowLevelClient[F, Request[F], Response[F]] {
      def run[Output](request: Request[F])(cb: Response[F] => F[Output]) =
        client.run(request).use(cb)
    }
}
