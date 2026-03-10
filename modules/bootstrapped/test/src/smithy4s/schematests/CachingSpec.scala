package smithy4s.schematests

import smithy4s.schema.Compilation
import smithy4s.schema._
import smithy4s.Lazy
import smithy4s.Refinement
import smithy4s.{Hints, ShapeId}
import smithy4s.schema.Alt
import smithy4s.Bijection
import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import munit.FunSuite

final class CachingSpec extends FunSuite {

  test("Caching works as intended for normal schemas"){
    case class Foo(int: Int, str: String)
    object Foo {
      val schema: Schema[Foo] =  {
        val int = Schema.int.required[Foo]("foo", _.int)
        val str = Schema.string.required[Foo]("str", _.str)
        struct(int, str)(Foo.apply)
      }
    }
    val treeCompilation = TreeVisitor.compile(Foo.schema)
    val tree = Compilation.expensiveRun(treeCompilation)
    assertEquals(tree.size, 2)
  }

  test("Caching works as intended for cyclic schemas".only){
    case class Foo(foo: Foo)
    object Foo {
      val schema: Schema[Foo] = recursive {
        val foos = schema.required[Foo]("foo", _.foo)
        struct(foos)(Foo.apply)
      }
    }
    val treeCompilation = TreeVisitor.compile(Foo.schema.transformHintsLocally(_.add(smithy.api.Documentation("foo"))))
    val tree = Compilation.expensiveRun(treeCompilation)
    assertEquals(tree.size, 3)
  }

}

sealed trait Tree {
  def size = Tree.flatten(this, Set.empty).size
}

object Tree {
  type Const[A] = Tree

  case class Node(children : IndexedSeq[Tree]) extends Tree
  case class Cycle(f : Lazy[Tree]) extends Tree

  val empty: Tree = Node(IndexedSeq.empty)
  def apply[A](trees: Tree*): Tree = Node(trees.toIndexedSeq)
  def flatten(tree: Tree, acc: Set[Tree]) : Set[Tree] = {
    if (acc(tree)) acc
    else  tree match {
      case n @ Node(children) =>
        children.foldLeft(acc + n){(currentAcc, child) =>
          currentAcc ++ flatten(child, currentAcc)
        }
      case c @ Cycle(lt) =>
        flatten(lt.value, acc + c)
    }
  }
}

object TreeVisitor extends Compilation.Visitor[Tree.Const] {
  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): Compilation[Tree] = leaf(Tree.empty)

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): Compilation[Tree] =
    compile(member).map(Tree(_))

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): Compilation[Tree] =
    compile(key).zip(compile(value)).map { case (kt, vt) => Tree(kt, vt) }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): Compilation[Tree] = leaf(Tree.empty)

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): Compilation[Tree] =
    Compilation
      .sequence(
        fields
          .map(f => compile(f.schema.asInstanceOf[Schema[Any]]))
          .toIndexedSeq
      )
      .map(Tree.Node(_))

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      ordinal: U => Int
  ): Compilation[Tree] = Compilation
    .sequence(
      alternatives
        .map(f => compile(f.schema.asInstanceOf[Schema[Any]]))
        .toIndexedSeq
    )
    .map(Tree.Node(_))

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): Compilation[Tree] = compile(schema).map(Tree(_))

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): Compilation[Tree] = compile(schema).map(Tree(_))

  def lazily[A](suspend: Lazy[Schema[A]]): Compilation[Tree] = {
    buildRecursive(suspend)(Tree.Cycle(_))
  }

  def option[A](schema: Schema[A]): Compilation[Tree] =
    compile(schema).map(Tree(_))

}
