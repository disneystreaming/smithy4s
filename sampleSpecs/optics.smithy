$version: "2"

namespace smithy4s.example

use smithy4s.meta#generateOptics

@generateOptics
union OpticsUnion {
  one: OpticsStructure
}

@generateOptics
structure OpticsStructure {
  two: OpticsEnum
}

@generateOptics
enum OpticsEnum {
  A, B
}

@generateOptics
structure TestInput {
    @required
    @httpLabel
    @length(min: 10)
    pathParam: String
    @httpQuery("queryParam")
    @length(min: 10)
    queryParam: String
    @httpPayload
    @required
    body: TestBody
}

@generateOptics
structure TestBody {
    @length(min: 10)
    data: String
}
