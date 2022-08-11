package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object Name extends Newtype[smithy4s.example.refined.Name] {
  val id: ShapeId = ShapeId("smithy4s.example", "Name")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[smithy4s.example.refined.Name] = string.refined[smithy4s.example.refined.Name](smithy4s.example.NameFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[Name] = bijection(underlyingSchema, asBijection)
}