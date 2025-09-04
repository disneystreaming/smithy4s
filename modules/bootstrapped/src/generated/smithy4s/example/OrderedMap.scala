package smithy4s.example

import scala.collection.immutable.ListMap
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.seqMap
import smithy4s.schema.Schema.string

object OrderedMap extends Newtype[ListMap[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "OrderedMap")
  val hints: Hints = Hints(
    alloy.PreserveKeyOrder(),
  ).lazily
  val underlyingSchema: Schema[ListMap[String, String]] = seqMap(string, string).withId(id).addHints(hints)
  implicit val schema: Schema[OrderedMap] = bijection(underlyingSchema, asBijection)
}
