package smithy4s.grpc

sealed trait GrpcDiscriminator extends Product with Serializable

object GrpcDiscriminator {
  final case class StatusCode(int: Int) extends GrpcDiscriminator
}
