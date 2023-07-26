$version: "2"

namespace smithy4s.example

union OpticsUnion {
  one: OpticsStructure
}

structure OpticsStructure {
  two: OpticsEnum
}

enum OpticsEnum {
  A, B
}

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

structure TestBody {
    @length(min: 10)
    data: String
}
