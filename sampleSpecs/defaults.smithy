$version: "2.0"

metadata defaultRenderMode = "FULL"

namespace smithy4s.example

list StringList {
  member: String
}

structure DefaultTest {
  one: Integer = 1
  two: String = "test"
  three: StringList = []
}

structure DefaultOrderingTest {
  one: Integer = 1
  two: String
  @required
  three: String
}
