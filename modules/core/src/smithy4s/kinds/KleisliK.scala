package smithy4s.kinds

import smithy4s.capability.{Covariant, MonadThrowLike}
import smithy4s.~>

/**
 * An effectful natural transformation. KleisliK is a functor transformation from `A[T]` to `B[T]` where `B[T]` is computed
 * in some effect `F`. KleisliK represents a function `forSome T => A[T] => F[B[T]]`.
 */
case class KleisliK[F[_], A[_], B[_]](run: A ~> KleisliK.FGA[F, B, *]) {
  def mapB[C[_]](f: B ~> C)(implicit F: Covariant[F]): KleisliK[F, A, C] =
    KleisliK(new (A ~> KleisliK.FGA[F, C, *]) {
      override def apply[T](at: A[T]): KleisliK.FGA[F, C, T] =
        F.map(run(at))(gt => f(gt))
    })

  def compose[C[_]](f: KleisliK[F, C, A])(implicit
                                          F: MonadThrowLike[F]
  ): KleisliK[F, C, B] = KleisliK(new (C ~> KleisliK.FGA[F, B, *]) {
    override def apply[T](ct: C[T]): KleisliK.FGA[F, B, T] =
      F.flatMap(f.run(ct))(at => run(at))
  })

  def andThen[C[_]](f: KleisliK[F, B, C])(implicit
                                          F: MonadThrowLike[F]
  ): KleisliK[F, A, C] =
    f.compose(this)

  def flatMap[C[_]](f: B ~> KleisliK.Unwrapped[F,A,C,*])(implicit
                                                         F: MonadThrowLike[F]
  ): KleisliK[F, A, C] = KleisliK(new (A ~> KleisliK.FGA[F, C, *]) {
    override def apply[T](at: A[T]): KleisliK.FGA[F, C, T] =
      F.flatMap(run(at))(bt => f(bt)(at))
  })
}
object KleisliK {
  type FGA[F[_], G[_], A] = F[G[A]]
  type Unwrapped[F[_], A[_], B[_], T] = A[T] => F[B[T]]
}