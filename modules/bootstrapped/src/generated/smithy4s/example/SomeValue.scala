package smithy4s.example

import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object SomeValue extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "SomeValue"))

  implicit val schema: Schema[SomeValue] = bijection(underlyingSchema, asBijection)
}
