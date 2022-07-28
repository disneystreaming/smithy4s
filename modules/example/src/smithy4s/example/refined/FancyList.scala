package smithy4s.example.refined

import smithy4s._

final case class FancyList private (values: List[String])

object FancyList {

  def apply(values: List[String]): Either[String, FancyList] =
    if (values.size > 10) Right(new FancyList(values))
    else Left("FancyLists must have more than 10 items")

  val provider = Refinement.drivenBy[smithy4s.example.FancyListFormat](
    FancyList.apply,
    (b: FancyList) => b.values
  )
}
