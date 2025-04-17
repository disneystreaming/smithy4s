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


@mixin()
structure MixinOptionalMember {
  a: String
}

structure MixinOptionalMemberOverride with [MixinOptionalMember] {
  @required
  a: String
}

structure MixinOptionalMemberDefaultAdded with [MixinOptionalMember] {
  a: String = "test"
}

// regression test for https://github.com/disneystreaming/smithy4s/issues/1699
@mixin
structure MixinRequiredMember {
  @required description: String
}

structure MixinRequiredMemberDefaultAdded with [MixinRequiredMember] {
  $description = "different description"
}
