package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object UVIndex extends Newtype[Int] {
  val id: ShapeId = ShapeId("smithy4s.example", "UVIndex")
  val hints: Hints = Hints(
    smithy.api.Default(_root_.smithy4s.Document.fromDouble(0.0d)),
  )
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[UVIndex] = bijection(underlyingSchema, asBijection)
}
