package smithy4s.example

import scala.collection.mutable.Map
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.linkedHashMap
import smithy4s.schema.Schema.string

object OrderedMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderedMap")
  val hints: Hints = Hints(
    alloy.PreserveKeyOrder(),
  ).lazily
  val underlyingSchema: Schema[Map[String, String]] = linkedHashMap(string, string).withId(id).addHints(hints)
  implicit val schema: Schema[OrderedMap] = bijection(underlyingSchema, asBijection)
}
