package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object SomeVector extends Newtype[Vector[String]] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "SomeVector")
  val hints : smithy4s.Hints = smithy4s.Hints()
  val underlyingSchema : smithy4s.Schema[Vector[String]] = vector(string).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[SomeVector] = bijection(underlyingSchema, asBijection)
}