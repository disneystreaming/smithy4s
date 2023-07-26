package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object Name extends Newtype[smithy4s.refined.Name] {
  val underlyingSchema: Schema[smithy4s.refined.Name] = string.refined[smithy4s.refined.Name](NameFormat())
  .withId(ShapeId("smithy4s.example", "Name"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[Name] = bijection(underlyingSchema, asBijection)
}
