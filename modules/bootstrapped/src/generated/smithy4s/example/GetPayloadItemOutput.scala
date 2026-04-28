package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetPayloadItemOutput(item: UnpackedItem)

object GetPayloadItemOutput extends ShapeTag.Companion[GetPayloadItemOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetPayloadItemOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(item: UnpackedItem): GetPayloadItemOutput = GetPayloadItemOutput(item)

  implicit val schema: Schema[GetPayloadItemOutput] = struct[GetPayloadItemOutput](
    UnpackedItem.schema.required[GetPayloadItemOutput]("item", _.item).addHints(Hints.dynamic(ShapeId("smithy.api", "httpPayload"), smithy4s.Document.obj())),
  )(make).withId(id).addHints(hints)
}
