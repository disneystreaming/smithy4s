// NOT Generated - kept here because of a circular dependency
// between this and the generated code
package smithy4s.refined

import smithy4s._
import smithy4s.example.NonEmptyListFormat

final case class NonEmptyList[A] private (values: List[A])

object NonEmptyList {

  def apply[A](values: List[A]): Either[String, NonEmptyList[A]] =
    if (values.size > 0) Right(new NonEmptyList(values))
    else Left("List must not be empty.")

  implicit def provider[A]
      : RefinementProvider[NonEmptyListFormat, List[A], NonEmptyList[A]] =
    Refinement.drivenBy[smithy4s.example.NonEmptyListFormat](
      NonEmptyList.apply[A],
      (b: NonEmptyList[A]) => b.values
    )
}
