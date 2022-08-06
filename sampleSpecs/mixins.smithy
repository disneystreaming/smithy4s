$version: "2.0"

namespace smithy4s.example

@mixin
structure CommonFieldsOne {
  a: String
  b: Integer
}

@mixin
structure CommonFieldsTwo {
  c: Long
}

structure MixinExample with [CommonFieldsOne, CommonFieldsTwo] {
  c: Long
  d: Boolean
}
