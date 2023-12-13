package smithy4s.example

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.byte

object StreamedBlob extends Newtype[Byte] {
  val id: ShapeId = ShapeId("smithy4s.example", "StreamedBlob")
  val hints: Hints = Hints.lazily(
    Hints(
      smithy.api.Streaming(),
    )
  )
  val underlyingSchema: Schema[Byte] = byte.withId(id).addHints(hints)
  implicit val schema: Schema[StreamedBlob] = bijection(underlyingSchema, asBijection)
}
