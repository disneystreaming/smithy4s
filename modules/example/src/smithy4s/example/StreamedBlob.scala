package smithy4s.example

import smithy4s._
import smithy4s.schema.Schema._

object StreamedBlob extends Newtype[Byte] {
  val id: ShapeId = ShapeId("smithy4s.example", "StreamedBlob")
  val hints : Hints = Hints(
    smithy.api.Streaming(),
  )
  val underlyingSchema : Schema[Byte] = byte.withId(id).addHints(hints)
  implicit val schema : Schema[StreamedBlob] = bijection(underlyingSchema, asBijection)
}