package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object SomeVector extends Newtype[Vector[String]] {
  val id: ShapeId = ShapeId("smithy4s.example", "SomeVector")
  val hints : Hints = Hints()
  val underlyingSchema : Schema[Vector[String]] = vector(string).withId(id).addHints(hints)
  implicit val schema : Schema[SomeVector] = bijection(underlyingSchema, asBijection)
}