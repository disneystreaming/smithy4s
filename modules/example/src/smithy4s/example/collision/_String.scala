package smithy4s.example.collision

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object _String extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example.collision", "String")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : Schema[_String] = bijection(underlyingSchema, asBijection)
}