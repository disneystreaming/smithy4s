package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object UVIndex extends Newtype[Int] {
  val hints: Hints = Hints(
    smithy.api.Default(smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[Int] = int.withId(ShapeId("smithy4s.example", "UVIndex")).addHints(hints)
  implicit val schema: Schema[UVIndex] = bijection(underlyingSchema, asBijection)
}
