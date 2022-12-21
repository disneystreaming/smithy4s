package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object Name extends Newtype[smithy4s.example.refined.Name] {
  val id: ShapeId = ShapeId("smithy4s.example", "Name")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[smithy4s.example.refined.Name] = string.refined[smithy4s.example.refined.Name](smithy4s.example.NameFormat()).withId(id).addHints(hints)
  implicit val schema: Schema[Name] = bijection(underlyingSchema, asBijection)
}