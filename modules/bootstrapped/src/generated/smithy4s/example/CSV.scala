package smithy4s.example

import smithy.api.MediaType
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object CSV extends Newtype[String] {
  val underlyingSchema: Schema[String] = string
  .withId(ShapeId("smithy4s.example", "CSV"))
  .addHints(
    Hints(
      MediaType("text/csv"),
    )
  )

  implicit val schema: Schema[CSV] = bijection(underlyingSchema, asBijection)
}
