package smithy4s.internals

import smithy4s.capability.{Cache, CacheWrite, MonadThrowLike}
import smithy4s.data.Tree
import smithy4s.schema.{Schema, VisitorF}
import smithy4s.~>

/**
 * A schema visitor that that constructs a path of trees.
 */
sealed abstract class TreeSchemaVisitor[F[+_], G[_]] {
  def cache: CacheWrite[F, G[_]]
  def fromSchema[A](schema: Schema[A], cacheRead: Cache[F, G[_]]): F[G[A]]
  final def visit[A](
      schema: Schema[A]
  )(implicit F: MonadThrowLike[F]): F[G[A]] = {
    val initTree = TreeSchemaVisitor.unfoldToTree(schema)
    println(Tree.draw(initTree)(s => s"${s.shapeId.name}: ${s.getClass.getSimpleName}"))
    TreeSchemaVisitor
      .cachedPostOrder(initTree, cache)(fromSchema(_, _))
      .asInstanceOf[F[G[A]]]
  }
}
object TreeSchemaVisitor extends TreeSchemaVisitorFunctions {
  type FGLambda[F[_], G[_], A] = F[G[A]]
  def make[F[+_], G[_]](
      f: Schema ~> FGLambda[F, G, *],
      makeCache: F[CacheWrite[F, G[_]]]
  )(implicit M: MonadThrowLike[F]): F[TreeSchemaVisitor[F, G]] =
    M.map(makeCache)(newCache =>
      new TreeSchemaVisitor[F, G] {
        override val cache: CacheWrite[F, G[_]] = newCache
        override def fromSchema[A](
            schema: Schema[A],
            cacheRead: Cache[F, G[_]]
        ): F[G[A]] = f(schema)
      }
    )

  def make[F[+_], G[_]](
      visitorF: VisitorF[G],
      makeCache: F[CacheWrite[F,G[_]]]
  )(implicit M: MonadThrowLike[F]): F[TreeSchemaVisitor[F, G]] =
    M.map(makeCache)(newCache =>
      new TreeSchemaVisitor[F,G] {
        override val cache: CacheWrite[F, G[_]] = newCache
        override def fromSchema[A](schema: Schema[A], cacheRead: Cache[F, G[_]]): F[G[A]] =
          visitorF.apply(schema, cacheRead)
      }
    )
}

sealed trait TreeSchemaVisitorFunctions {
  def unfoldToTree[A](
      schema: Schema[A],
      initCache: Set[Schema[_]] = Set.empty
  ): Tree[Schema[_]] = {
    def children(schema: Schema[_]): Vector[Schema[_]] = schema match {
      case Schema.PrimitiveSchema(_, _, _)          => Vector.empty
      case Schema.CollectionSchema(_, _, _, member) => Vector(member)
      case Schema.MapSchema(_, _, _, key, value)       => Vector(key, value)
      case Schema.EnumerationSchema(_, _, _, _)  => Vector.empty
      case Schema.StructSchema(_, _, fields, _)     => fields.map(f => f.schema)
      case Schema.UnionSchema(_, _, alts, _)      => alts.map(alt => alt.schema)
      case Schema.OptionSchema(_, underlying)        => Vector(underlying)
      case Schema.BijectionSchema(underlying, _)  => Vector(underlying)
      case Schema.RefinementSchema(underlying, _) => Vector(underlying)
      case Schema.LazySchema(suspend)             => Vector(suspend.value)
    }
    def loop(tree: Tree[Schema[_]], cache: Set[Schema[_]]): Tree[Schema[_]] = {
      tree match {
        case l @ Tree.Leaf(value) if cache.contains(value) =>
          l
        case l @ Tree.Leaf(value) =>
          val lChildren =
            children(value).map(child => loop(Tree.Leaf(child), cache + value))
          if (lChildren.isEmpty) {
            l
          } else {
            Tree.Branch(value, lChildren)
          }
        case b @ Tree.Branch(value, _) if cache.contains(value) =>
          println("Branch cache hit!")
          println(Tree.draw(b)(debugShow(_)))
          println("======")
          b
        case Tree.Branch(value, ts) =>
          Tree.Branch(value, ts.map(t => loop(t, cache + value)))
      }
    }
    val init = Tree.Leaf(schema)
    loop(init, initCache)
  }

  def cachedPostOrder[F[_], A](tree: Tree[Schema[_]], cache: CacheWrite[F, A])(
      f: (Schema[_], Cache[F, A]) => F[A]
  )(implicit F: MonadThrowLike[F]): F[A] = {
    def sequence(va: Vector[F[A]]): F[Vector[A]] = {
      if (va.isEmpty) {
        F.pure(Vector.empty)
      } else {
        F.flatMap(va.head)(a =>
          F.map(sequence(va.tail))(vecA => vecA.prepended(a))
        )
      }
    }
    tree match {
      case Tree.Leaf(value) =>
        F.flatMap(f(value, cache))(a => F.map(cache.put(value, a))(_ => a))
      case Tree.Branch(value, ts) =>
        val children = sequence(ts.map(t => cachedPostOrder(t, cache)(f)))
        F.flatMap(F.flatMap(children)(_ => f(value, cache)))(a =>
          F.map(cache.put(value, a))(_ => a)
        )
    }
  }

  def debugShow[A](s: Schema[A]): String = s"${s.shapeId.name}: ${s.getClass.getSimpleName}"
}
