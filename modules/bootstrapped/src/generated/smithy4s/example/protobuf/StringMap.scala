package smithy4s.example.protobuf

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.map
import smithy4s.schema.Schema.string

object StringMap extends Newtype[Map[String, Int]] {
  val id: ShapeId = ShapeId("smithy4s.example.protobuf", "StringMap")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Map[String, Int]] = map(string, int).withId(id).addHints(hints)
  implicit val schema: Schema[StringMap] = bijection(underlyingSchema, asBijection)
}
