package smithy4s

package object kinds {

  // format: off
  // Not using type projectors as it messes with type inference when using the algebra
  type FunctorAlgebra[Alg[_[_, _, _, _, _]], F[_]] = Alg[[I, E, O, SI, SO] =>> F[O]]
  type FunctorInterpreter[Op[_, _, _, _, _], F[_]] = PolyFunction5[Op, [I, E, O, SI, SO] =>> F[O]]
  type BiFunctorAlgebra[Alg[_[_, _, _, _, _]], F[_, _]] = Alg[[I, E, O, SI, SO] =>> F[E, O]]
  type BiFunctorInterpreter[Op[_, _, _, _, _], F[_, _]] = PolyFunction5[Op, [I, E, O, SI, SO] =>> F[E, O]]
  // format: on

  type Kind1[F[_]] = {
    type toKind2[E, O] = F[O]
    type toKind5[I, E, O, SI, SO] = F[O]
  }

  type Kind2[F[_, _]] = {
    type toKind5[I, E, O, SI, SO] = F[E, O]
  }

}
