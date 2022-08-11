package smithy4s.example

import smithy4s.example.refined.Age.provider._
import smithy4s._
import smithy4s.schema.Schema._

object Age extends Newtype[smithy4s.example.refined.Age] {
  val id: ShapeId = ShapeId("smithy4s.example", "Age")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[smithy4s.example.refined.Age] = int.refined[smithy4s.example.refined.Age](smithy4s.example.AgeFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[Age] = bijection(underlyingSchema, asBijection)
}