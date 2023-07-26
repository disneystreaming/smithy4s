package smithy4s.example

import smithy.api.Streaming
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.byte

object StreamedBlob extends Newtype[Byte] {
  val underlyingSchema: Schema[Byte] = byte
  .withId(ShapeId("smithy4s.example", "StreamedBlob"))
  .addHints(
    Hints(
      Streaming(),
    )
  )

  implicit val schema: Schema[StreamedBlob] = bijection(underlyingSchema, asBijection)
}
