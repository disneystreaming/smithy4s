package smithy4s.example.guides

package object hello {
  type HelloWorldService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[HelloWorldServiceGen, F]
  val HelloWorldService = HelloWorldServiceGen


}