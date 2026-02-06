package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class GenericClientError(message: String) extends Smithy4sThrowable {
  override def getMessage(): String = message
}

object GenericClientError extends ShapeTag.Companion[GenericClientError] {
  val id: ShapeId = ShapeId("smithy4s.example", "GenericClientError")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "error"), smithy4s.Document.fromString("client")),
    Hints.dynamic(ShapeId("smithy.api", "httpError"), smithy4s.Document.fromLong(418)),
  )

  // constructor using the original order from the spec
  private def make(message: String): GenericClientError = GenericClientError(message)

  implicit val schema: Schema[GenericClientError] = struct(
    string.required[GenericClientError]("message", _.message),
  )(make).withId(id).addHints(hints)
}
