$version: "2.0"

namespace smithy4s.example

list StringList {
  member: String
}

structure DefaultTest {
  one: Integer = 1
  two: String = "test"
  three: StringList = []
}
