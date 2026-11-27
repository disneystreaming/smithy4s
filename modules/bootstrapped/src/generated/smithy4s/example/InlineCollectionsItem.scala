package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class InlineCollectionsItem(name: Option[String] = None)

object InlineCollectionsItem extends ShapeTag.Companion[InlineCollectionsItem] {
  val id: ShapeId = ShapeId("smithy4s.example", "InlineCollectionsItem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(name: Option[String]): InlineCollectionsItem = InlineCollectionsItem(name)

  implicit val schema: Schema[InlineCollectionsItem] = struct[InlineCollectionsItem](
    string.optional[InlineCollectionsItem]("name", _.name),
  )(make).withId(id).addHints(hints)
}
