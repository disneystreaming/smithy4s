package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetOptionalItemOutput(item: Option[UnpackedItem] = None)

object GetOptionalItemOutput extends ShapeTag.Companion[GetOptionalItemOutput] {
  val id: ShapeId = ShapeId("smithy4s.example", "GetOptionalItemOutput")

  val hints: Hints = Hints(
    Hints.dynamic(ShapeId("smithy.api", "output"), smithy4s.Document.obj()),
  )

  // constructor using the original order from the spec
  private def make(item: Option[UnpackedItem]): GetOptionalItemOutput = GetOptionalItemOutput(item)

  implicit val schema: Schema[GetOptionalItemOutput] = struct[GetOptionalItemOutput](
    UnpackedItem.schema.optional[GetOptionalItemOutput]("item", _.item),
  )(make).withId(id).addHints(hints)
}
