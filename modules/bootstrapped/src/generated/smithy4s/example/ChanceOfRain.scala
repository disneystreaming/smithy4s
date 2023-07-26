package smithy4s.example

import smithy.api.Default
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.float

object ChanceOfRain extends Newtype[Float] {
  val id: ShapeId = ShapeId("smithy4s.example", "ChanceOfRain")
  val hints: Hints = Hints(
    Default(smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[Float] = float.withId(id).addHints(hints)
  implicit val schema: Schema[ChanceOfRain] = bijection(underlyingSchema, asBijection)
}
