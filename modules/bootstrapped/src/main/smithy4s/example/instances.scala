package smithy4s.example

import smithy4s.RefinementProvider

object instances {

  implicit val lengthProviderValidatedRefinedPrimitive
      : RefinementProvider.Simple[
        smithy.api.Length,
        Name
      ] =
    RefinementProvider.lengthConstraint(_.value.value.length)
}
