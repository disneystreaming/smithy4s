package smithy4s.example.collision

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map

object MyMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "MyMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, String]] = map(String.schema, String.schema).withId(id).addHints(hints)
  implicit val schema: Schema[MyMap] = bijection(underlyingSchema, asBijection)
}
