package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object Menu extends Newtype[Map[String, MenuItem]] {
  val id: ShapeId = ShapeId("smithy4s.example", "Menu")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, MenuItem]] = map(string, MenuItem.schema).withId(id).addHints(hints)
  implicit val schema: Schema[Menu] = bijection(underlyingSchema, asBijection)
}
