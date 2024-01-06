package smithy4s.example

import _root_.smithy4s.Hints
import _root_.smithy4s.Newtype
import _root_.smithy4s.Schema
import _root_.smithy4s.ShapeId
import _root_.smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.byte

object StreamedBlob extends Newtype[Byte] {
  val id: ShapeId = ShapeId("smithy4s.example", "StreamedBlob")
  val hints: Hints = Hints(
    smithy.api.Streaming(),
  )
  val underlyingSchema: Schema[Byte] = byte.withId(id).addHints(hints)
  implicit val schema: Schema[StreamedBlob] = bijection(underlyingSchema, asBijection)
}
