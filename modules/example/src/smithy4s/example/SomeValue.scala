package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object SomeValue extends Newtype[String] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeValue")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[String] = string.withId(id).addHints(hints)
  implicit val schema : Schema[SomeValue] = bijection(underlyingSchema, asBijection)
}