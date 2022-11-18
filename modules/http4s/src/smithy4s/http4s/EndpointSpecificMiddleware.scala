package smithy4s
package http4s

import org.http4s.HttpApp

// format: off
trait EndpointSpecificMiddleware[F[_]] {
  def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): HttpApp[F] => HttpApp[F]
}
// format: on

object EndpointSpecificMiddleware {

  trait Simple[F[_]] extends EndpointSpecificMiddleware[F] {
    def prepareUsingHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[F] => HttpApp[F]

    final def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
        endpoint: Endpoint[service.Operation, _, _, _, _, _]
    ): HttpApp[F] => HttpApp[F] =
      prepareUsingHints(service.hints, endpoint.hints)
  }

  private[http4s] type EndpointMiddleware[F[_], Op[_, _, _, _, _]] =
    Endpoint[Op, _, _, _, _, _] => HttpApp[F] => HttpApp[F]

  def noop[F[_]]: EndpointSpecificMiddleware[F] =
    new EndpointSpecificMiddleware[F] {
      override def prepare[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] = identity
    }

}
