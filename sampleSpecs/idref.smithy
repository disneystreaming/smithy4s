$version: "2"

namespace smithy4s.example

structure TestIdRef {
  @idRef
  test: String
  test2: TestIdRefTwo
}

@idRef
string TestIdRefTwo

union TestIdRefUnion {
  @idRef
  test: String
  testTwo: TestIdRefTwo
}

list TestIdRefList {
  @idRef
  member: String
}

map TestIdRefKeyMap {
  @idRef
  key: String
  value: String
}

map TestIdRefValueMap {
  key: String
  @idRef
  value: String
}
