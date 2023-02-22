package smithy4s.http4s.kernel

import org.http4s.Response

trait ResponseEncoder[F[_], A] {
  def addToResponse(response: Response[F], a: A): Response[F]
}
