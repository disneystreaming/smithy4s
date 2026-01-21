package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** JSON payload type */
object JsonPayload extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "JsonPayload")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("JSON payload type")),
    Hints.dynamic(ShapeId("smithy.api", "mediaType"), smithy4s.Document.fromString("application/json")),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[JsonPayload] = bijection(underlyingSchema, asBijection)
}
