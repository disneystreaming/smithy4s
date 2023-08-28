package smithy4s.http4s.internals

import org.http4s.client.Client
import org.http4s.{Request, Response}
import smithy4s.client.UnaryClient
import cats.effect.MonadCancelThrow

private[internals] object Http4sToSmithy4sClient {
  def apply[F[_]: MonadCancelThrow](
      client: Client[F]
  ): UnaryClient[F, Request[F], Response[F]] =
    new UnaryClient[F, Request[F], Response[F]] {
      def run[Output](request: Request[F])(cb: Response[F] => F[Output]) =
        client.run(request).use(cb)
    }
}
