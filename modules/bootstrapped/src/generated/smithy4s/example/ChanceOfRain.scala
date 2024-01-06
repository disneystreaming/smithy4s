package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.float

object ChanceOfRain extends Newtype[Float] {
  val id: ShapeId = ShapeId("smithy4s.example", "ChanceOfRain")
  val hints: Hints = Hints(
    smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[Float] = float.withId(id).addHints(hints)
  implicit val schema: Schema[ChanceOfRain] = bijection(underlyingSchema, asBijection)
}
