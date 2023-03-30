package smithy4s.http4s.kernel

import org.http4s.Request

trait RequestDecoder[F[_], A] {
  def decodeRequest(request: Request[F]): F[A]
}
