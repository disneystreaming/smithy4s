package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetRequiredItemOutput(item: UnpackedItem)

object GetRequiredItemOutput extends ShapeTag.Companion[GetRequiredItemOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetRequiredItemOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(item: UnpackedItem): GetRequiredItemOutput = GetRequiredItemOutput(item)

  implicit val schema: Schema[GetRequiredItemOutput] = struct[GetRequiredItemOutput](
    UnpackedItem.schema.required[GetRequiredItemOutput]("item", _.item),
  )(make).withId(id).addHints(hints)
}
