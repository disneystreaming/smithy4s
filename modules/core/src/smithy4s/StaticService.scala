package smithy4s

import smithy4s.kinds.FunctorK5
import smithy4s.kinds.PolyFunction5

trait StaticService[P[_[_, _, _, _, _]]] extends FunctorK5[P] {
  type Alg[_[_, _, _, _, _]]
  val service: Service[Alg]
  def endpoints: P[service.Endpoint]

  def toPolyFunction[P2[_, _, _, _, _]](
      algebra: P[P2]
  ): PolyFunction5[service.Endpoint, P2]
}

object StaticService {

  type Aux[Alg[_[_, _, _, _, _]], P[_[_, _, _, _, _]]] =
    StaticService[Alg] {
      type Alg[Op[_, _, _, _, _]] = P[Op]
    }

}

trait Mirror[Alg[_[_, _, _, _, _]]] {
  type StaticAlg[_[_, _, _, _, _]]
  val static: StaticService.Aux[StaticAlg, Alg]
}

object Mirror {

  type Aux[Alg[_[_, _, _, _, _]], SAlg[_[_, _, _, _, _]]] =
    Mirror[Alg] {
      type StaticAlg[Op[_, _, _, _, _]] = SAlg[Op]
    }

}
