package smithy4s.example.refined

import smithy4s._
import smithy4s.example.NameFormat

final case class Name private (value: String)

object Name {

  def apply(value: String): Either[String, Name] =
    if (value.size > 1) Right(new Name(value))
    else Left("Name length must be > 1")

  implicit val provider: RefinementProvider[NameFormat, String, Name] = Refinement.drivenBy[smithy4s.example.NameFormat](
    Name.apply,
    (b: Name) => b.value
  )
}
