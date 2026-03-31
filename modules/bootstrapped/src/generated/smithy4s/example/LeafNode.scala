package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class LeafNode(value: Int)

object LeafNode extends ShapeTag.Companion[LeafNode] {
  val id: ShapeId = ShapeId("smithy4s.example", "LeafNode")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(value: Int): LeafNode = LeafNode(value)

  implicit val schema: Schema[LeafNode] = struct[LeafNode](
    int.required[LeafNode]("value", _.value),
  )(make).withId(id).addHints(hints)
}
