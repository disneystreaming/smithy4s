$version: "2.0"

metadata smithy4sRenderValidatedNewtypes = true

namespace newtypes.validated

use smithy4s.meta#unwrap
use alloy#simpleRestJson

@length(min: 1, max: 10)
string ValidatedCity

@length(min: 1, max: 10)
string ValidatedName

@unwrap
@length(min: 1, max: 10)
string ValidatedCountry

structure Person {
  @httpLabel
  @required
  name: ValidatedName

  @httpQuery("town")
  town: ValidatedCity

  @httpQuery("country")
  country: ValidatedCountry
}
