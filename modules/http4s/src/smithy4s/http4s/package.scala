package smithy4s

import org.http4s.HttpApp
import cats.kernel.Monoid
import org.http4s.client.Client

package object http4s {
  type ServerEndpointMiddleware[F[_]] = Endpoint.Middleware[HttpApp[F]]
  type ClientEndpointMiddleware[F[_]] = Endpoint.Middleware[Client[F]]

  implicit def monoidEndpointMiddleware[Construct]
      : Monoid[Endpoint.Middleware[Construct]] =
    new Monoid[Endpoint.Middleware[Construct]] {
      def combine(
          a: Endpoint.Middleware[Construct],
          b: Endpoint.Middleware[Construct]
      ): Endpoint.Middleware[Construct] =
        a.andThen(b)

      val empty = Endpoint.Middleware.noop
    }
}
