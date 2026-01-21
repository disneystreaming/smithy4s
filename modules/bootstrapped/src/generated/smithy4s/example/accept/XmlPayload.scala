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
    Hints.dynamic(ShapeId("smithy.api", "documentation"), smithy4s.Document.fromString("XML payload type")),
    Hints.dynamic(ShapeId("smithy.api", "mediaType"), smithy4s.Document.fromString("application/xml")),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[XmlPayload] = bijection(underlyingSchema, asBijection)
}
