package smithy4s.example

import smithy4s._
import smithy4s.example.refined.Name
import smithy4s.schema.Schema._

object DogName extends Newtype[Name] {
  val id: ShapeId = ShapeId("smithy4s.example", "DogName")
  val hints : Hints = Hints.empty
  val underlyingSchema : Schema[Name] = string.refined[smithy4s.example.refined.Name](smithy4s.example.NameFormat()).withId(id).addHints(hints)
  implicit val schema : Schema[DogName] = bijection(underlyingSchema, asBijection)
}