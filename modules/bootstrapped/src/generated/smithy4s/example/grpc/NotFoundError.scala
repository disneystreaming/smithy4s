package smithy4s.example.grpc

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class NotFoundError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object NotFoundError extends ShapeTag.Companion[NotFoundError] {
  val id: ShapeId = ShapeId("smithy4s.example.grpc", "NotFoundError")

  val hints: Hints = Hints(
    alloy.proto.GrpcError(code = 5, message = Some("Resource not found")),
    smithy.api.Error.CLIENT.widen,
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): NotFoundError = NotFoundError(message)

  implicit val schema: Schema[NotFoundError] = struct(
    string.required[NotFoundError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
