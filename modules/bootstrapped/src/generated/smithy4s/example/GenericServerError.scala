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
    Hints.dynamic(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("server")),
    Hints.dynamic(ShapeId("smithy.api", "httpError"), smithy4s.Document.fromLong(502L)),
  )

  // constructor using the original order from the spec
  private def make(message: String): GenericServerError = GenericServerError(message)

  implicit val schema: Schema[GenericServerError] = struct[GenericServerError](
    string.required[GenericServerError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
