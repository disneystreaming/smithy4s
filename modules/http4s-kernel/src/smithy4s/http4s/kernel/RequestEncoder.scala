package smithy4s.http4s.kernel

import org.http4s.Request

trait RequestEncoder[F[_], A] {
  def addToRequest(request: Request[F], a: A): Request[F]
}
