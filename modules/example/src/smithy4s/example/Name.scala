package smithy4s.example

import smithy4s.Newtype
import smithy4s.example.refined.Name._
import smithy4s.schema.Schema._

object Name extends Newtype[smithy4s.example.refined.Name] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "Name")
  val hints : smithy4s.Hints = smithy4s.Hints.empty
  val underlyingSchema : smithy4s.Schema[smithy4s.example.refined.Name] = string.refined(smithy4s.example.NameFormat()).withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[Name] = bijection(underlyingSchema, asBijection)
}