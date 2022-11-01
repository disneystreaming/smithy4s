$version: "2.0"

namespace foo

use alloy#uuidFormat

structure Foo {
  a: Integer
}

@uuidFormat
string MyUUID
