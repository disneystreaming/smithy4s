$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#defaultRender

list StringList {
  member: String
}

structure DefaultTest {
  one: Integer = 1
  two: String = "test"
  three: StringList = []
}

@defaultRender(mode: "NONE")
structure DefaultNone {
  one: Integer = 1
  two: String
  @required
  three: String
}

@defaultRender(mode: "OPTION_ONLY")
structure DefaultOptionOnly {
  one: Integer = 1
  two: String
  @required
  three: String
}

@defaultRender(mode: "FULL")
structure DefaultFull {
  one: Integer = 1
  two: String
  @required
  three: String
}
