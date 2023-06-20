package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object CSV extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "CSV")
  val hints: Hints = Hints(
    smithy.api.MediaType("text/csv"),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[CSV] = bijection(underlyingSchema, asBijection)
}
