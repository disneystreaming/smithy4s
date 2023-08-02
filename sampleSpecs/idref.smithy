$version: "2"

namespace smithy4s.example

structure TestIdRef {
  @idRef
  test: String
  test2: TestIdRefTwo
}

@idRef
string TestIdRefTwo
