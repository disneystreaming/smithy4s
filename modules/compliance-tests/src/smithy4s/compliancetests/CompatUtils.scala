package smithy4s.compliancetests

import cats.Monoid
import cats.MonadError
import cats.Monad

abstract class CompatUtils[F[_]: MonadError[*[_], Throwable]] {
  def raiseError[A](err: Throwable): F[A] =
    MonadError[F, Throwable].raiseError(err)

  implicit def monoid[A: Monoid]: Monoid[F[A]] = new Monoid[F[A]] {
    override def combine(x: F[A], y: F[A]): F[A] =
      Monad[F].flatMap(x)(xa =>
        Monad[F].map(y)(ya => Monoid[A].combine(xa, ya))
      )
    override def empty: F[A] = Monad[F].pure(Monoid[A].empty)
  }
}
