package smithy4s.example.refined

import smithy4s._
import smithy4s.example.AgeFormat

final case class Age private (value: Int)

object Age {

  def apply(value: Int): Either[String, Age] =
    if (value > 0) Right(new Age(value))
    else Left("Age must be > 0")

  // Done like this just to test the import functionality. Not normally recommended.
  object provider {
    implicit val provider: RefinementProvider[AgeFormat, Int, Age] = Refinement.drivenBy[smithy4s.example.AgeFormat](
      Age.apply,
      (b: Age) => b.value
    )
  }
}
