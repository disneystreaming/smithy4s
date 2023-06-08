package smithy4s

import smithy4s.kinds.FunctorK5
import smithy4s.kinds.PolyFunction5

trait ServiceProduct[Prod[_[_, _, _, _, _]]] extends FunctorK5[Prod] {
  type Alg[_[_, _, _, _, _]]
  val service: Service[Alg]
  def endpointsProduct: Prod[service.Endpoint]

  def toPolyFunction[Prod2[_, _, _, _, _]](
      algebra: Prod[Prod2]
  ): PolyFunction5[service.Endpoint, Prod2]
}

object ServiceProduct {

  type Aux[Alg[_[_, _, _, _, _]], Prod[_[_, _, _, _, _]]] =
    ServiceProduct[Alg] {
      type Alg[Op[_, _, _, _, _]] = Prod[Op]
    }

}

trait Mirror[Alg[_[_, _, _, _, _]]] {
  type Prod[_[_, _, _, _, _]]
  val serviceProduct: ServiceProduct.Aux[Prod, Alg]
}

object Mirror {

  type Aux[Alg[_[_, _, _, _, _]], ProdAlg[_[_, _, _, _, _]]] =
    Mirror[Alg] {
      type Prod[Op[_, _, _, _, _]] = ProdAlg[Op]
    }

}
