package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object SomeValue extends Newtype[String] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "SomeValue")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[SomeValue] = bijection(underlyingSchema, SomeValue(_), (_ : SomeValue).value)
}