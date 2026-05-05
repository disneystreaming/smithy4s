package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class UnpackedItem(id: String)

object UnpackedItem extends ShapeTag.Companion[UnpackedItem] {
  val id: ShapeId = ShapeId("smithy4s.example", "UnpackedItem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(id: String): UnpackedItem = UnpackedItem(id)

  implicit val schema: Schema[UnpackedItem] = struct[UnpackedItem](
    string.required[UnpackedItem]("id", _.id),
  )(make).withId(id).addHints(hints)
}
