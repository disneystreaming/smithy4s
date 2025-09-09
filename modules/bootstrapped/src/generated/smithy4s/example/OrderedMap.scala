package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object OrderedMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderedMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, String]] = map(string, string).withId(id).addHints(hints)
  implicit val schema: Schema[OrderedMap] = bijection(underlyingSchema, asBijection)
}
