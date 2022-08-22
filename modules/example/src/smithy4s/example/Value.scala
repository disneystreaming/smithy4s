package smithy4s.example

import smithy4s.Schema
import smithy4s.schema.Schema.int
import smithy4s.Hints
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.Newtype

object Value extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "Value")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : Schema[Value] = bijection(underlyingSchema, asBijection)
}