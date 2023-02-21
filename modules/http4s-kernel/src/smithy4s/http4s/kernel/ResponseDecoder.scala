package smithy4s.http4s.kernel

import org.http4s.Response

trait ResponseDecoder[F[_], A] {
  def decodeResponse(request: Response[F]): F[A]
}
