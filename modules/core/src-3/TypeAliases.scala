package smithy4s

protected[smithy4s] trait TypeAliases {

  type Monadic[Alg[_[_, _, _, _, _]], F[_]] =
    Alg[[I, E, O, SI, SO] =>> F[O]]

  type Interpreter[Op[_, _, _, _, _], F[_]] =
    Transformation[Op, [I, E, O, SI, SO] =>> F[O]]

}
