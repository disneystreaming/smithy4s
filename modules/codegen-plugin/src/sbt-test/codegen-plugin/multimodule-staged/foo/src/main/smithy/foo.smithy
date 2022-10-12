$version: "2.0"

namespace foo

use smithy4s.api#uuidFormat

structure Foo {
  a: Integer
}

@uuidFormat
string MyUUID
