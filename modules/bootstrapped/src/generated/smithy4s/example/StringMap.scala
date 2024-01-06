package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object StringMap extends Newtype[Map[String, String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "StringMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, String]] = map(string, string).withId(id).addHints(hints)
  implicit val schema: Schema[StringMap] = bijection(underlyingSchema, asBijection)
}
