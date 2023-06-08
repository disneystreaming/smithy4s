package smithy4s

import smithy4s.kinds.FunctorK5
import smithy4s.kinds.PolyFunction5

/**
  * Something that returns a product of endpoints.
  * Contains the same information as a service, with the difference that the
  * algebra (the product type parameter) can be be an interface with methods
  * without inputs.
  * 
  * @tparam Prod the product type parameter. For code generation this is also
  * generated as an interface with methods without inputs (one for each
  * endpoint). This has suffix `ProductGen`.
  */
trait ServiceProduct[Prod[_[_, _, _, _, _]]] extends FunctorK5[Prod] {
  type Alg[_[_, _, _, _, _]]
  val service: Service[Alg]
  def endpointsProduct: Prod[service.Endpoint]

  def toPolyFunction[Func[_, _, _, _, _]](
      algebra: Prod[Func]
  ): PolyFunction5[service.Endpoint, Func]
}

object ServiceProduct {

  type Aux[Alg[_[_, _, _, _, _]], Prod[_[_, _, _, _, _]]] =
    ServiceProduct[Alg] {
      type Alg[Op[_, _, _, _, _]] = Prod[Op]
    }

}

/**
  * Provides a way to get the product version for a service.
  * For info about the `Prod` type parameter, see [[ServiceProduct]].
  * 
  * @tparam Alg the algebra type parameter.
  */
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
