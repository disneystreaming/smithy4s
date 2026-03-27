package smithy4s.capability

import smithy4s.Lazy
import smithy4s.schema.Schema

import scala.collection.concurrent.TrieMap

/**
 * A typeclass representing the capabaility to memoize values of type `A` against a given schema.
 */
trait Cache[F[_], A] {
  def get(schema: Schema[_]): F[Lazy[A]]
}

trait CacheWrite[F[_], A] extends Cache[F, A] {
  def put(schema: Schema[_], a: A): F[Unit]
  def size: F[Int]
  def clear: F[Unit]
}
object CacheWrite {
  // A Naive implementation, adapt this to scalacache in the near future.
  def make[F[_], A](implicit F: SyncLike[F], M: MonadThrowLike[F]): F[CacheWrite[F, A]] = {
    M.map(F.delay(TrieMap.empty[Schema[_], A])) { mut =>
      new CacheWrite[F, A] {
        override def clear: F[Unit] = F.delay(mut.clear())
        override def put(schema: Schema[_], a: A): F[Unit] =
          M.map(F.delay(mut.put(schema, a)))(_ => ())
        override def get(schema: Schema[_]): F[Lazy[A]] =
          M.pure(Lazy(mut(schema)))
        override def size: F[Int] = F.delay(mut.size)
      }
    }
  }
}