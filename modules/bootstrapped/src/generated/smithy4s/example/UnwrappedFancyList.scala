package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list
import smithy4s.schema.Schema.string

object UnwrappedFancyList extends Newtype[smithy4s.refined.FancyList] {
  val underlyingSchema: Schema[smithy4s.refined.FancyList] = list(string).refined[smithy4s.refined.FancyList](FancyListFormat())
  .withId(ShapeId("smithy4s.example", "UnwrappedFancyList"))
  .addHints(
    Hints.empty
  )

  implicit val schema: Schema[UnwrappedFancyList] = bijection(underlyingSchema, asBijection)
}
