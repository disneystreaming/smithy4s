$version: "2.0"

namespace foo

use alloy#uuidFormat
use aws.api#data

structure Foo {
  a: Integer
}

@data("tagging")
@uuidFormat
string MyUUID
