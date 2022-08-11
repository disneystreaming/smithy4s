$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#adtMember

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

@error("client")
structure MixinErrorExample with [CommonFieldsOne, CommonFieldsTwo] {
  c: Long
  d: Boolean
}

@mixin
structure EmptyMixin {}

structure TestEmptyMixin with [EmptyMixin] {
  a: Long
}

union TestMixinAdt {
  test: TestAdtMemberWithMixin
}

@adtMember(TestMixinAdt)
structure TestAdtMemberWithMixin with [CommonFieldsOne] {}
