package smithy4s.capability

/**
  * A typeclass abstracting over the notion of encoder.
  *
  * Useful in particular when encoding unions
  */
trait EncoderK[F[_], Result] extends Contravariant[F] {
  def apply[A](fa: F[A], a: A): Result
  def absorb[A](f: A => Result): F[A]
  def contramap[A, B](fa: F[A])(f: B => A): F[B] =
    absorb[B](b => apply[A](fa, f(b)))
}
