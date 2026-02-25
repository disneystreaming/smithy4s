package smithy4s.example

package object grpc {
  type GrpcGreetingService[F[_]] = smithy4s.kinds.FunctorAlgebra[GrpcGreetingServiceGen, F]
  val GrpcGreetingService = GrpcGreetingServiceGen


}