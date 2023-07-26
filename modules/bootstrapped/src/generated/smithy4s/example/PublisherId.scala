package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object PublisherId extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "PublisherId"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[PublisherId] = bijection(underlyingSchema, asBijection)
}
