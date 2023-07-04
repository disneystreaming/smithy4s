package smithy4s.kinds

object PolyFunctions {

  def mapErrorK[E, E1](f: E => E1): PolyFunction[Either[E, *], Either[E1, *]] =
    new PolyFunction[Either[E, *], Either[E1, *]] {
      def apply[A](either: Either[E, A]): Either[E1, A] = either.left.map(f)
    }

}
