package smithy4s.example

package object test {
  type HelloService[F[_]] = smithy4s.kinds.FunctorAlgebra[HelloServiceGen, F]
  val HelloService = HelloServiceGen
  type HelloWorldService[F[_]] = smithy4s.kinds.FunctorAlgebra[HelloWorldServiceGen, F]
  val HelloWorldService = HelloWorldServiceGen


}