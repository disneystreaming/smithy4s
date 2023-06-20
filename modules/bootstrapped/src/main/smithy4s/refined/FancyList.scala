// NOT Generated - kept here because of a circular dependency
// between this and the generated code
package smithy4s.refined

import smithy4s._
import smithy4s.example.FancyListFormat

final case class FancyList private (values: List[String])

object FancyList {

  def apply(values: List[String]): Either[String, FancyList] =
    if (values.size > 10) Right(new FancyList(values))
    else Left("FancyLists must have more than 10 items")

  implicit val provider
      : RefinementProvider[FancyListFormat, List[String], FancyList] =
    Refinement.drivenBy[smithy4s.example.FancyListFormat](
      FancyList.apply,
      (b: FancyList) => b.values
    )
}
