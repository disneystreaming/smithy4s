package smithy4s.example

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema.union

sealed trait Tree extends scala.Product with scala.Serializable { self =>
  @inline final def widen: Tree = this
  def $ordinal: Int

  object project {
    def tree: Option[TreeNode] = Tree.TreeCase.alt.project.lift(self).map(_.tree)
    def leaf: Option[LeafNode] = Tree.LeafCase.alt.project.lift(self).map(_.leaf)
  }

  def accept[A](visitor: Tree.Visitor[A]): A = this match {
    case value: Tree.TreeCase => visitor.tree(value.tree)
    case value: Tree.LeafCase => visitor.leaf(value.leaf)
  }
}
object Tree extends ShapeTag.Companion[Tree] {

  def tree(tree: TreeNode): Tree = TreeCase(tree)
  def leaf(leaf: LeafNode): Tree = LeafCase(leaf)

  val id: ShapeId = ShapeId("smithy4s.example", "Tree")

  val hints: Hints = Hints.empty

  final case class TreeCase(tree: TreeNode) extends Tree { final def $ordinal: Int = 0 }
  final case class LeafCase(leaf: LeafNode) extends Tree { final def $ordinal: Int = 1 }

  object TreeCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Tree.TreeCase] = bijection(TreeNode.schema.addHints(hints), Tree.TreeCase(_), _.tree)
    val alt = schema.oneOf[Tree]("tree")
  }
  object LeafCase {
    val hints: Hints = Hints.empty
    val schema: Schema[Tree.LeafCase] = bijection(LeafNode.schema.addHints(hints), Tree.LeafCase(_), _.leaf)
    val alt = schema.oneOf[Tree]("leaf")
  }

  trait Visitor[A] {
    def tree(value: TreeNode): A
    def leaf(value: LeafNode): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def tree(value: TreeNode): A = default
      def leaf(value: LeafNode): A = default
    }
  }

  implicit val schema: Schema[Tree] = recursive(union(
    Tree.TreeCase.alt,
    Tree.LeafCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints))
}
