package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericServerError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object GenericServerError extends ShapeTag.Companion[GenericServerError] {
  val id: ShapeId = ShapeId("smithy4s.example", "GenericServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(502),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: String): GenericServerError = GenericServerError(message)

  implicit val schema: Schema[GenericServerError] = struct(
    string.required[GenericServerError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
