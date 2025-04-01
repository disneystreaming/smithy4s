package smithy4s.example

import smithy4s.Document
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.document
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object AdditionalProperties extends Newtype[Map[String, Document]] {
  val id: ShapeId = ShapeId("smithy4s.example", "AdditionalProperties")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Document]] = map(string, document).withId(id).addHints(hints)
  implicit val schema: Schema[AdditionalProperties] = bijection(underlyingSchema, asBijection)
}
