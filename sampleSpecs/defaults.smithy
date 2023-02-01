$version: "2.0"

metadata smithy4sDefaultRenderMode = "FULL"

namespace smithy4s.example

list StringList {
  member: String
}

map DefaultStringMap {
  key: String
  value: String
}

structure DefaultTest {
  one: Integer = 1
  two: String = "test"
  three: StringList = []
  @default
  four: StringList
  @default
  five: String
  @default
  six: Integer
  @default
  seven: Document
  @default
  eight: DefaultStringMap
  @default
  nine: Short
  @default
  ten: Double
  @default
  eleven: Float
  @default
  twelve: Long
}

structure DefaultOrderingTest {
  one: Integer = 1
  two: String
  @required
  three: String
}

@mixin()
structure DefaultInMixinTest {
  one: String = "test"
}

structure DefaultInMixinUsageTest with [DefaultInMixinTest] {}
