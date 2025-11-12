package smithy4s

import munit.FunSuite

import smithy4s.example.{
  Tree,
  TreeNode,
  LeafNode,
  Foo,
  ConsList,
  Cons,
  Nil => Nill
}
import cats.Hash
import smithy4s.schema._
import scala.annotation.tailrec
import smithy4s.internals.maps.MMap
import smithy4s.interopcats.SchemaVisitorHash

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
                List(Tree.tree(TreeNode(left, right)))
              case x => x
            }
            .toList
          recursiveFold(joined)
        }
      }

    recursiveFold(nodes)
  }

  def buildConsList(size: Int): ConsList = {
    (1 to size).foldLeft(ConsList.nil(Nill()))((list, i) =>
      ConsList.cons(Cons(i, list))
    )
  }

  def useLazyTestCache[F[_]](store: MMap[Any, Any]) = new CompilationCache[F] {
    override def getOrElseUpdate[A](
        schema: Schema[A],
        fetch: Schema[A] => F[A]
    ): F[A] = {
      store.getOrElseUpdate(schema, fetch(schema)).asInstanceOf[F[A]]
    }
  }

  def ignoreLazyTestCache[F[_]](store: MMap[Any, Any]) =
    new CompilationCache[F] {
      override def getOrElseUpdate[A](
          schema: Schema[A],
          fetch: Schema[A] => F[A]
      ): F[A] = {
        if (schema.isInstanceOf[Schema.LazySchema[_]]) { fetch(schema) }
        else store.getOrElseUpdate(schema, fetch(schema)).asInstanceOf[F[A]]
      }
    }

  def runTest[A](
      caseString: String,
      value: Int => A,
      buildCache: MMap[Any, Any] => CompilationCache[Hash],
      transformSchema: Schema[A] => Schema[A] = (x: Schema[A]) => x
  )(implicit schema: Schema[A]) =
    test(s"$caseString case") {
      val store: MMap[Any, Any] = MMap.empty
      val updatedSchema = transformSchema(schema)

      val hashVisitor: Hash[A] =
        SchemaVisitorHash.fromSchema(updatedSchema, buildCache(store))

      val sizeAfterVisitor = store.size
      val sizes = List(1, 10, 100, 1000, 100, 10, 1)
      val storeSizeAfterHashing = sizes.map(i => {
        hashVisitor.hash(value(i))
        store.size
      })

      println(
        s"$caseString: afterVisitor=$sizeAfterVisitor, afterHashing=$storeSizeAfterHashing"
      )

      assert(true)
    }

  def addHints[A](schema: Schema[A]): Schema[A] =
    schema.transformHintsTransitively(hints =>
      hints.add(smithy.api.Documentation("Adding some hints"))
    )

  def runTestCases[A: Schema](caseString: String, value: Int => A) = {
    runTest(
      s"$caseString current cache, hints unchanged",
      value,
      buildCache = ignoreLazyTestCache
    )
    runTest(
      s"$caseString current cache, hints transformed",
      value,
      buildCache = ignoreLazyTestCache,
      transformSchema = addHints[A]
    )
    runTest(
      s"$caseString updated cache, hints unchanged",
      value,
      buildCache = useLazyTestCache
    )
    runTest(
      s"$caseString updated cache, hints transformed",
      value,
      buildCache = useLazyTestCache,
      transformSchema = addHints[A]
    )
  }

  runTestCases("Foo", Foo.int)
  runTestCases("Tree", buildTree)
  runTestCases("Cons", buildConsList)

}
