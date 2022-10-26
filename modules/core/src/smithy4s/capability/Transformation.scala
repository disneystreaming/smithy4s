package smithy4s
package capability

import kinds._

/**
  * Heterogenous function construct, allows to abstract over various kinds of functions
  * whilst providing an homogenous user experience without the user having to
  * manually lift functions from one kind to the other.
  *
  * Used to reduce the noise of transformations
  */
trait Transformation[Func, Input, Output] {
  def apply(f: Func, input: Input): Output
}

object Transformation {

  // format: off
  implicit def functorK5_poly1_transformation[Alg[_[_, _, _, _, _]]: FunctorK5, F[_], G[_]]: Transformation[PolyFunction[F, G], FunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]] =
    new Transformation[PolyFunction[F, G], FunctorAlgebra[Alg, F], FunctorAlgebra[Alg, G]]{
      def apply(func: PolyFunction[F, G], algF: FunctorAlgebra[Alg, F]) : FunctorAlgebra[Alg, G] = FunctorK5[Alg].mapK5[Kind1[F]#toKind5, Kind1[G]#toKind5](algF, toPolyFunction5(func))
    }

  implicit def functorK5_poly2_transformation[Alg[_[_, _, _, _, _]]: FunctorK5, F[_,_], G[_, _]]: Transformation[PolyFunction2[F, G], BiFunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]] =
    new Transformation[PolyFunction2[F, G], BiFunctorAlgebra[Alg, F], BiFunctorAlgebra[Alg, G]]{
      def apply(func: PolyFunction2[F, G], algF: BiFunctorAlgebra[Alg, F]) : BiFunctorAlgebra[Alg, G] = FunctorK5[Alg].mapK5[Kind2[F]#toKind5, Kind2[G]#toKind5](algF, toPolyFunction5(func))
    }


  class PartiallyApplied[Input](input: Input) {
    def apply[Func, Output](func: Func)(implicit
        transform: Transformation[Func, Input, Output]
    ) = transform(func, input)
  }

}
