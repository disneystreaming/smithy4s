package smithy4s.example

import smithy4s.Newtype
import smithy4s.schema.Schema._

object StreamedBlob extends Newtype[Byte] {
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "StreamedBlob")
  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Streaming(),
  )
  val underlyingSchema : smithy4s.Schema[Byte] = byte.withId(id).addHints(hints)
  implicit val schema : smithy4s.Schema[StreamedBlob] = bijection(underlyingSchema, StreamedBlob(_), (_ : StreamedBlob).value)
}