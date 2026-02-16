package smithy4s.grpc

import smithy4s.ShapeId

sealed trait GrpcDiscriminator extends Product with Serializable

object GrpcDiscriminator {
  final case class ByShapeId(shapeId: ShapeId) extends GrpcDiscriminator
  case object Undetermined extends GrpcDiscriminator
}
