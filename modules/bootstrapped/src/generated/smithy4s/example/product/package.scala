package smithy4s.example

package object product {
  type ExampleService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ExampleServiceGen, F]
  val ExampleService = ExampleServiceGen


}