package smithy4s.example

package object greet {
  type GreetService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[GreetServiceGen, F]
  val GreetService = GreetServiceGen


}