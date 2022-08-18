package smithy4s.example

import smithy4s.Schema
import smithy4s.Hints
import smithy4s.schema.Schema.string
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object SomeValue extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeValue")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : Schema[SomeValue] = bijection(underlyingSchema, asBijection)
}