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

object RetainedUnknownFields extends Newtype[Map[String, Document]] {
  val id: ShapeId = ShapeId("smithy4s.example", "RetainedUnknownFields")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Document]] = map(string, document).withId(id).addHints(hints)
  implicit val schema: Schema[RetainedUnknownFields] = bijection(underlyingSchema, asBijection)
}
