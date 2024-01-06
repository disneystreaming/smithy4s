package smithy4s.example

import _root_.smithy4s.Blob
import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.bytes

object PNG extends Newtype[Blob] {
  val id: ShapeId = ShapeId("smithy4s.example", "PNG")
  val hints: Hints = Hints(
    smithy.api.MediaType("image/png"),
  )
  val underlyingSchema: Schema[Blob] = bytes.withId(id).addHints(hints)
  implicit val schema: Schema[PNG] = bijection(underlyingSchema, asBijection)
}
