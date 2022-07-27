package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object PersonAge extends Newtype[Int] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PersonAge")
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy4s.example.AgeFormat(),
  )
  val underlyingSchema : smithy4s.Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[PersonAge] = bijection(underlyingSchema, asBijection)
}