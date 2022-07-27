package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object PersonAge extends Newtype[smithy4s.example.refined.Age] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "PersonAge")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[smithy4s.example.refined.Age] = int.refined(smithy4s.example.AgeFormat())(smithy4s.example.refined.Age.provider).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[PersonAge] = bijection(underlyingSchema, asBijection)
}