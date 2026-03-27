package smithy4s.capability

/**
 * A typeclass with a cats.effect.Sync.delay like constructor
 * @tparam F
 */
trait SyncLike[F[_]] {
  def delay[A](thunk: => A): F[A]
}