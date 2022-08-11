package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object ObjectSize extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "ObjectSize")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : Schema[ObjectSize] = bijection(underlyingSchema, asBijection)
}