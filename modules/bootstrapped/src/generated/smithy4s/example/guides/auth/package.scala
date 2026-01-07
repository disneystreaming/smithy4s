package smithy4s.example.guides

package object auth {
  type HelloWorldAuthService[F[_]] = smithy4s.kinds.FunctorAlgebra[HelloWorldAuthServiceGen, F]
  val HelloWorldAuthService = HelloWorldAuthServiceGen


}