package smithy4s.example

package object product {
  type ExampleService[F[_]] = smithy4s.kinds.FunctorAlgebra[ExampleServiceGen, F]
  val ExampleService = ExampleServiceGen


}