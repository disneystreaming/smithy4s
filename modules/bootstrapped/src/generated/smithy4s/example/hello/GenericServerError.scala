package smithy4s.example.hello

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericServerError(message: Option[String] = None) extends Smithy4sThrowable {
  override def getMessage(): String = message.orNull
}

object GenericServerError extends ShapeTag.Companion[GenericServerError] {
  val id: ShapeId = ShapeId("smithy4s.example.hello", "GenericServerError")

  val hints: Hints = Hints(
    smithy.api.Error.SERVER.widen,
    smithy.api.HttpError(500),
  ).lazily

  // constructor using the original order from the spec
  private def make(message: Option[String]): GenericServerError = GenericServerError(message)

  implicit val schema: Schema[GenericServerError] = struct(
    string.optional[GenericServerError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
