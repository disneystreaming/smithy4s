package smithy4s

package object kinds {

  // format: off
  type FunctorAlgebra[Alg[_[_, _, _, _, _]], F[_]] = Alg[Kind1[F]#toKind5]
  type FunctorInterpreter[Op[_, _, _, _, _], F[_]] = PolyFunction5[Op, Kind1[F]#toKind5]
  type BiFunctorAlgebra[Alg[_[_, _, _, _, _]], F[_, _]] = Alg[Kind2[F]#toKind5]
  type BiFunctorInterpreter[Op[_, _, _, _, _], F[_,_]] = PolyFunction5[Op, Kind2[F]#toKind5]
  // format: on

  type Kind1[F[_]] = {
    type toKind2[E, O] = F[O]
    type toKind5[I, E, O, SI, SO] = F[O]
  }

  type Kind2[F[_, _]] = {
    type toKind5[I, E, O, SI, SO] = F[E, O]
  }

}
