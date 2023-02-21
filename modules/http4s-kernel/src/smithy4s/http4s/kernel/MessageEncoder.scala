package smithy4s.http4s.kernel

import org.http4s.Request
import org.http4s.Response

trait MessageEncoder[F[_], A] {
  def encodeRequest(a: A): Request[F]
  def encodeResponse(a: A): Response[F]
}
