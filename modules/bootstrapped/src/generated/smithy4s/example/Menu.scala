package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object Menu extends Newtype[Map[String, MenuItem]] {
  val id: ShapeId = ShapeId("smithy4s.example", "Menu")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, MenuItem]] = map(string, MenuItem.schema).withId(id).addHints(hints)
  implicit val schema: Schema[Menu] = bijection(underlyingSchema, asBijection)
}
