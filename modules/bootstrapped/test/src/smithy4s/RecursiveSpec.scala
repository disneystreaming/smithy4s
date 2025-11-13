package smithy4s

import munit.FunSuite

import smithy4s.example.{Tree, TreeNode, LeafNode, Foo}
import cats.Hash
import smithy4s.schema._
import scala.annotation.tailrec
import smithy4s.internals.maps.MMap
import smithy4s.interopcats.SchemaVisitorHash
import smithy4s.schema.Schema.recursive
import smithy4s.schema.Schema._

class RecursiveSpec extends FunSuite {

  case class Recurse(n: Option[Recurse])

  object Recurse {
    implicit val schema: Schema[Recurse] = recursive {
      struct(
        schema.optional[Recurse]("n", _.n)
      )(Recurse.apply)
    }
  }

  def buildTree(size: Int): Tree = {
    val seed = Math.round(Math.random() * 1000).toInt
    val nodes = (1 to size).map(i => Tree.leaf(LeafNode(i * seed))).toList

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

  def buildRecursive(size: Int): Recurse =
    (1 to size).foldLeft(Recurse(None))((tail, i) => Recurse(Some(tail)))

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
    test(s"$caseString") {
      val store: MMap[Any, Any] = MMap.empty

      val updatedSchema = transformSchema(schema)

      val hashVisitor: Hash[A] =
        SchemaVisitorHash.fromSchema(updatedSchema, buildCache(store))

      // Invoke hash with a size that will have some recursion so that the build cache an be materialized
      hashVisitor.hash(value(2))
      val sizeAfterInitializing = store.size

      val sizes = List(10, 100, 256)
      sizes.foreach(i => hashVisitor.hash(value(i)))
      val sizeAfterHashing = store.size

      assertEquals(
        sizeAfterHashing,
        sizeAfterInitializing,
        "cache store size has grown after initialization"
      )
    }

  def addHints[A](schema: Schema[A]): Schema[A] = {
    schema.transformHintsTransitively(
      _.add(
        smithy.api.Documentation("Adding some hints")
      )
    )
  }

  def runTestCases[A: Schema](caseString: String, value: Int => A) = {
    runTest(
      s"$caseString: current cache, hints unchanged",
      value,
      buildCache = ignoreLazyTestCache
    )
    runTest(
      s"$caseString: current cache, hints transformed",
      value,
      buildCache = ignoreLazyTestCache,
      transformSchema = addHints[A]
    )
    runTest(
      s"$caseString: updated cache, hints unchanged",
      value,
      buildCache = useLazyTestCache
    )
    runTest(
      s"$caseString: updated cache, hints transformed",
      value,
      buildCache = useLazyTestCache,
      transformSchema = addHints[A]
    )
  }

  runTestCases("Foo", Foo.int)
  runTestCases("Tree", buildTree)
  runTestCases("Recurse", buildRecursive)

}
