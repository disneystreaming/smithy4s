package smithy4s.example

import smithy.api.Box
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object ObjectSize extends Newtype[Int] {
  val underlyingSchema: Schema[Int] = int
  .withId(ShapeId("smithy4s.example", "ObjectSize"))
  .addHints(
    Hints(
      Box(),
    )
  )

  implicit val schema: Schema[ObjectSize] = bijection(underlyingSchema, asBijection)
}
