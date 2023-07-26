package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object Menu extends Newtype[Map[String, MenuItem]] {
  val underlyingSchema: Schema[Map[String, MenuItem]] = map(string, MenuItem.schema)
  .withId(ShapeId("smithy4s.example", "Menu"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[Menu] = bijection(underlyingSchema, asBijection)
}
