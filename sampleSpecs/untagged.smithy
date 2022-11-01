namespace smithy4s.example

use alloy#untagged

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
