package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

/** A plain Smithy string */
object AString extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "AString")
  val hints: Hints = Hints(
    smithy.api.Documentation("A plain Smithy string"),
  )
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[AString] = bijection(underlyingSchema, asBijection)
}