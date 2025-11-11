package smithy4s

import munit.FunSuite

import smithy4s.example.{Tree, TreeNode, LeafNode}
import smithy4s.schema.CompilationCache
import scala.annotation.tailrec
import smithy4s.internals.maps.MMap

// import cats.Show
import smithy4s.interopcats.SchemaVisitorShow

class RecursiveSpec extends FunSuite {

  def buildTree(size: Int): Tree = {
    val nodes = List.unfold(1)(count => {
      if (count <= size) Some((Tree.leaf(LeafNode(count)), count + 1)) else None
    })

    @tailrec()
    def recursiveFold(els: List[Tree]): Tree =
      els match {
        case head :: Nil => head
        case x => {
          val joined: List[Tree] = x
            .sliding(2, 2)
            .flatMap {
              case left :: right :: Nil =>
                List(Tree.tree(TreeNode(Some(left), Some(right))))
              case x => x
            }
            .toList
          recursiveFold(joined)
        }
      }

    recursiveFold(nodes)
  }

  def testCache[F[_]](store: MMap[Any, Any]) = new CompilationCache[F] {
    override def getOrElseUpdate[A](
        schema: Schema[A],
        fetch: Schema[A] => F[A]
    ): F[A] = {
      // Lazy is tricky in that the thunk it contains can never be expressed
      // in a "stable" way, even in a dynamic context when most accessors/injectors can be
      // expressed in a serialisable fashion.
      if (schema.isInstanceOf[Schema.LazySchema[_]]) { fetch(schema) }
      else store.getOrElseUpdate(schema, fetch(schema)).asInstanceOf[F[A]]
    }
  }

  test("recursive doesn't blow up the stack") {
    val tree = buildTree(1024)
    val store: MMap[Any, Any] = MMap.empty

    val updatedSchema = Tree.schema.withId("exampole.test", "Tree2")

    val showVisitor =
      SchemaVisitorShow.fromSchema(updatedSchema, testCache(store))

    println(showVisitor.show(tree))
    println(store.size)
    assert(true)
  }

}
