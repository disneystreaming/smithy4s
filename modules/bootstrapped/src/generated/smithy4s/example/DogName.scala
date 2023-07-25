package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object DogName extends Newtype[smithy4s.refined.Name] {
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[smithy4s.refined.Name] = string.refined[smithy4s.refined.Name](smithy4s.example.NameFormat()).withId(ShapeId("smithy4s.example", "DogName")).addHints(hints)
  implicit val schema: Schema[DogName] = bijection(underlyingSchema, asBijection)
}
