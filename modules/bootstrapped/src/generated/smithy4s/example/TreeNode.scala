package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.struct

final case class TreeNode(left: Tree, right: Tree)

object TreeNode extends ShapeTag.Companion[TreeNode] {
  val id: ShapeId = ShapeId("smithy4s.example", "TreeNode")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(left: Tree, right: Tree): TreeNode = TreeNode(left, right)

  implicit val schema: Schema[TreeNode] = recursive(struct(
    Tree.schema.required[TreeNode]("left", _.left),
    Tree.schema.required[TreeNode]("right", _.right),
  )(make).withId(id).addHints(hints))
}
