package smithy4s.compliancetests

import smithy4s.Service
import cats.effect.Resource
import smithy4s.kinds.FunctorAlgebra
import smithy4s.http.CodecAPI
import smithy4s.ShapeTag
import org.http4s.HttpApp

/* A construct encapsulating the action of turning an http4s route into
 * an an algebra
 */
trait ReverseRouter[F[_]] {
  type Protocol
  def protocolTag: ShapeTag[Protocol]
  def codecs: CodecAPI

  def reverseRoutes[Alg[_[_, _, _, _, _]]](routes: HttpApp[F])(implicit
      service: Service[Alg]
  ): Resource[F, FunctorAlgebra[Alg, F]]
}
