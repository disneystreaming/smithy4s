namespace smithy4s.example

use smithy4s.api#untagged

@untagged
union UntaggedUnion {
  three: Three,
  four: Four
}

structure Three {
  @required
  three: String
}

structure Four {
  @required
  four: Integer
}
