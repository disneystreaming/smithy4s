package smithy4s.example.accept

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** XML payload type */
object XmlPayload extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.accept", "XmlPayload")
  val hints: Hints = Hints(
    smithy.api.Documentation("XML payload type"),
    smithy.api.MediaType("application/xml"),
  ).lazily
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[XmlPayload] = bijection(underlyingSchema, asBijection)
}
