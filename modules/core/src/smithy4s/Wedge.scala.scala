package smithy4s

sealed trait Wedge[+A, +B] {
  def orElse[A1 >: A, B1 >: B](other: => Wedge[A1, B1]): Wedge[A1, B1] =
    this match {
      case Wedge.Empty => other
      case _           => this
    }

  def toEither[A1 >: A, B1 >: B](whenEmpty: => Either[A1, B1]): Either[A1, B1] =
    this match {
      case Wedge.Left(a)  => Left(a)
      case Wedge.Right(b) => Right(b)
      case Wedge.Empty    => whenEmpty
    }
}

object Wedge {

  final case class Left[A](a: A) extends Wedge[A, Nothing]
  final case class Right[B](b: B) extends Wedge[Nothing, B]
  case object Empty extends Wedge[Nothing, Nothing]

}
