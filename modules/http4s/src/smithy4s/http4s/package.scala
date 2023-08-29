package smithy4s

import org.http4s.HttpApp
import org.http4s.client.Client

package object http4s {
  type ServerEndpointMiddleware[F[_]] = Endpoint.Middleware[HttpApp[F]]
  type ClientEndpointMiddleware[F[_]] = Endpoint.Middleware[Client[F]]
}
