package smithy4s.example.grpc

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class PermissionDeniedError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object PermissionDeniedError extends ShapeTag.Companion[PermissionDeniedError] {
  val id: ShapeId = ShapeId("smithy4s.example.grpc", "PermissionDeniedError")

  val hints: Hints = Hints(
    alloy.proto.GrpcError(code = 7, message = Some("Permission denied")),
    smithy.api.Error.CLIENT.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): PermissionDeniedError = PermissionDeniedError(message)

  implicit val schema: Schema[PermissionDeniedError] = struct(
    string.required[PermissionDeniedError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
