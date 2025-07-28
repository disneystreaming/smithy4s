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

@mixin
structure MixinRequiredMember {
  @required description: String
}

@mixin
structure MixinRequiredMemberIntermediate with [MixinRequiredMember] {
  extraField: String
}

// regression test for https://github.com/disneystreaming/smithy4s/issues/1702
structure StructUsingMixinRequiredMember with [MixinRequiredMemberIntermediate] {
    @required
    $extraField
}

// regression test for https://github.com/disneystreaming/smithy4s/issues/1699
structure MixinRequiredMemberDefaultAdded with [MixinRequiredMember] {
  $description = "different description"
}

@mixin
union NonStructureMixin {
  s: String
}
