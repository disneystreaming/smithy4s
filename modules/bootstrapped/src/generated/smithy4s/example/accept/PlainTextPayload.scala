package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** Plain text payload type */
object PlainTextPayload extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "PlainTextPayload")
  val hints: Hints = Hints(
    smithy.api.Documentation("Plain text payload type"),
    smithy.api.MediaType("text/plain"),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[PlainTextPayload] = bijection(underlyingSchema, asBijection)
}
