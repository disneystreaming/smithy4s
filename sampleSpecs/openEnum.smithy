$version: "2"

namespace smithy4s.example

use alloy#openEnum
use smithy4s.meta#generateOptics

@openEnum
@generateOptics
enum OpenEnumTest {
  ONE
}

@openEnum
@generateOptics
intEnum OpenIntEnumTest {
  ONE = 1
}
