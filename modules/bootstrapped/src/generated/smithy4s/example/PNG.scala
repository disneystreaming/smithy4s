package smithy4s.example

import smithy.api.MediaType
import smithy4s.ByteArray
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.bytes

object PNG extends Newtype[ByteArray] {
  val id: ShapeId = ShapeId("smithy4s.example", "PNG")
  val hints: Hints = Hints(
    MediaType("image/png"),
  )
  val underlyingSchema: Schema[ByteArray] = bytes.withId(id).addHints(hints)
  implicit val schema: Schema[PNG] = bijection(underlyingSchema, asBijection)
}
