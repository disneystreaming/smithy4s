package smithy4s.example

import smithy4s.Newtype
import smithy4s.syntax._

object StreamedBlob extends Newtype[Byte] {
  object T {
    val hints : smithy4s.Hints = smithy4s.Hints(
      id,
      smithy.api.Streaming(),
    )
    val schema : smithy4s.Schema[Byte] = byte.withHints(hints)
    implicit val staticSchema : schematic.Static[smithy4s.Schema[Byte]] = schematic.Static(schema)
  }
  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.example", "StreamedBlob")
  val hints: smithy4s.Hints = T.hints
  val schema : smithy4s.Schema[StreamedBlob] = bijection(T.schema, StreamedBlob(_), (_ : StreamedBlob).value)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[StreamedBlob]] = schematic.Static(schema)
}