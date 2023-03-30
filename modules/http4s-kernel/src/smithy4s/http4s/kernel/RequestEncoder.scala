package smithy4s.http4s.kernel

import org.http4s.Request

trait RequestEncoder[F[_], A] {
  def addToRequest(request: Request[F], a: A): Request[F]
}

object RequestEncoder {

  def empty[F[_], A]: RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] = request
  }

  def combine[F[_], A](
      left: RequestEncoder[F, A],
      right: RequestEncoder[F, A]
  ): RequestEncoder[F, A] = new RequestEncoder[F, A] {
    def addToRequest(request: Request[F], a: A): Request[F] =
      right.addToRequest(left.addToRequest(request, a), a)
  }

}
