package smithy4s.internals

import smithy4s.capability.{Cache, CacheWrite, MonadThrowLike}
import smithy4s.data.Tree
import smithy4s.internals.TreeBasedTraversal.FGLambda
import smithy4s.schema.{Schema, VisitorF, VisitorRF}
import smithy4s.~>

/**
 * Unfolds a schema into a rose tree of schema nodes, terminating recursion in the schemas by ending traversal when a
 * node is already been seen in current path from the root node.
 */
sealed abstract class TreeBasedTraversal[F[+_], G[_]] { self =>
  def cache: CacheWrite[F, G[_]]
  def fromSchema[A](schema: Schema[A], cacheRead: Cache[F, G[_]]): F[G[A]]
  final def visit[A](
      schema: Schema[A]
  )(implicit F: MonadThrowLike[F]): F[G[A]] = {
    val initTree = TreeBasedTraversal.unfoldToTree(schema)
    // println(Tree.draw(initTree)(TreeBasedTraversal.debugShow(_)))
    TreeBasedTraversal
      .cachedPostOrder(initTree, cache)(fromSchema(_, _))
      .asInstanceOf[F[G[A]]]
  }

  final def visitK(implicit
      F: MonadThrowLike[F]
  ): Schema ~> TreeBasedTraversal.FGLambda[F, G, *] =
    new (Schema ~> TreeBasedTraversal.FGLambda[F, G, *]) {
      override def apply[A0](schema: Schema[A0]): FGLambda[F, G, A0] = visit(schema)
    }
}
object TreeBasedTraversal extends TreeBasedTraversalFunctions {
  type FGLambda[F[_], G[_], A] = F[G[A]]
  def make[F[+_], G[_]](
      f: Schema ~> FGLambda[F, G, *],
      makeCache: F[CacheWrite[F, G[_]]]
  )(implicit M: MonadThrowLike[F]): F[TreeBasedTraversal[F, G]] =
    M.map(makeCache)(newCache =>
      new TreeBasedTraversal[F, G] {
        override val cache: CacheWrite[F, G[_]] = newCache
        override def fromSchema[A](
            schema: Schema[A],
            cacheRead: Cache[F, G[_]]
        ): F[G[A]] = f(schema)
      }
    )

  def make[F[+_], G[_]](
      visitorF: VisitorF[G],
      makeCache: F[CacheWrite[F, G[_]]]
  )(implicit M: MonadThrowLike[F]): F[TreeBasedTraversal[F, G]] =
    M.map(makeCache)(newCache =>
      new TreeBasedTraversal[F, G] {
        override val cache: CacheWrite[F, G[_]] = newCache
        override def fromSchema[A](
            schema: Schema[A],
            cacheRead: Cache[F, G[_]]
        ): F[G[A]] =
          visitorF(cacheRead).apply(schema)
      }
    )

  def make[F[+_], I[_], G[_]](
      visitorRF: VisitorRF[I, G],
      makeCache: F[CacheWrite[F, G[_]]],
      ask: Schema ~> I
  )(implicit M: MonadThrowLike[F]): F[TreeBasedTraversal[F, G]] =
    M.map(makeCache)(newCache =>
      new TreeBasedTraversal[F, G] {
        override val cache: CacheWrite[F, G[_]] = newCache
        override def fromSchema[A](
            schema: Schema[A],
            cacheRead: Cache[F, G[_]]
        ): F[G[A]] =
          visitorRF(cacheRead, ask).apply(schema)
      }
    )
}

sealed trait TreeBasedTraversalFunctions {
  def unfoldToTree[A](
      schema: Schema[A],
      initCache: Set[Schema[_]] = Set.empty
  ): Tree[Schema[_]] = {
    def children(schema: Schema[_]): Vector[Schema[_]] = schema match {
      case Schema.PrimitiveSchema(_, _, _)          => Vector.empty
      case Schema.CollectionSchema(_, _, _, member) => Vector(member)
      case Schema.MapSchema(_, _, _, key, value)    => Vector(key, value)
      case Schema.EnumerationSchema(_, _, _, _)     => Vector.empty
      case Schema.StructSchema(_, _, fields, _)     => fields.map(f => f.schema)
      case Schema.UnionSchema(_, _, alts, _)      => alts.map(alt => alt.schema)
      case Schema.OptionSchema(_, underlying)     => Vector(underlying)
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
        case b @ Tree.Branch(value, _) if cache.contains(value) => b
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

  def debugShow[A](s: Schema[A]): String =
    s"${s.shapeId.name}: ${s.getClass.getSimpleName}"
}
