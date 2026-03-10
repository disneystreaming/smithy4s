package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object UVIndex extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "UVIndex")
  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "default"), smithy4s.Document.fromLong(0L)),
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[UVIndex] = bijection(underlyingSchema, asBijection)
}
