package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object DogName extends Newtype[smithy4s.example.refined.Name] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "DogName")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[smithy4s.example.refined.Name] = string.refined(smithy4s.example.NameFormat())(smithy4s.example.refined.Name.provider).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[DogName] = bijection(underlyingSchema, asBijection)
}