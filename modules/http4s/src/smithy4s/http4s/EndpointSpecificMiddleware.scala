package smithy4s
package http4s

import org.http4s.HttpApp

// format: off
trait EndpointSpecificMiddleware[Alg[_[_, _, _, _, _]], F[_]] {
  def prepare(service: Service[Alg])(
      endpoint: Endpoint[service.Operation, _, _, _, _, _]
  ): HttpApp[F] => HttpApp[F]
}
// format: on

object EndpointSpecificMiddleware {

  trait Simple[Alg[_[_, _, _, _, _]], F[_]]
      extends EndpointSpecificMiddleware[Alg, F] {
    def prepareUsingHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[F] => HttpApp[F]

    final def prepare(service: Service[Alg])(
        endpoint: Endpoint[service.Operation, _, _, _, _, _]
    ): HttpApp[F] => HttpApp[F] =
      prepareUsingHints(service.hints, endpoint.hints)
  }

  private[http4s] type EndpointMiddleware[F[_], Op[_, _, _, _, _]] =
    Endpoint[Op, _, _, _, _, _] => HttpApp[F] => HttpApp[F]

  def noop[Alg[_[_, _, _, _, _]], F[_]]: EndpointSpecificMiddleware[Alg, F] =
    new EndpointSpecificMiddleware[Alg, F] {
      override def prepare(service: Service[Alg])(
          endpoint: Endpoint[service.Operation, _, _, _, _, _]
      ): HttpApp[F] => HttpApp[F] = identity
    }

}
