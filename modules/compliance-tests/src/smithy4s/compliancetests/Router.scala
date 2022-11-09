package smithy4s.compliancetests

import smithy4s.Service
import cats.effect.Resource
import org.http4s.HttpRoutes
import smithy4s.kinds.FunctorAlgebra
import smithy4s.ShapeTag

/* A construct encapsulating the action of turning an algebra implementation into
 * an http route (modelled using Http4s, but could be backed by any other library
 * by means of proxyfication)
 */
trait Router[F[_]] {
  type Protocol
  def protocolTag: ShapeTag[Protocol]

  def routes[Alg[_[_, _, _, _, _]]](alg: FunctorAlgebra[Alg, F])(implicit
      service: Service[Alg]
  ): Resource[F, HttpRoutes[F]]
}
