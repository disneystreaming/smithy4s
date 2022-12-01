package smithy4s.example.refined

import smithy4s._

final case class NonEmptyList[A] private (values: List[A])

object NonEmptyList {

  def apply[A](values: List[A]): Either[String, NonEmptyList[A]] =
    if (values.size > 0) Right(new NonEmptyList(values))
    else Left("List must not be empty.")

  implicit def provider[A] = Refinement.drivenBy[smithy4s.example.NonEmptyListFormat](
    NonEmptyList.apply[A],
    (b: NonEmptyList[A]) => b.values
  )
}
