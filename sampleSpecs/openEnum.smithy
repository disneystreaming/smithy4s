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

@openEnum
@generateOptics
enum OpenEnumCollisionTest {
  ONE,
  TWO,
  Unknown
}

@openEnum
@generateOptics
enum OpenEnumCollisionTest2 {
  ONE,
  TWO,
  THREE = "unknown"
}

@openEnum
@generateOptics
enum OpenEnumCollisionTest3 {
  ONE,
  TWO,
  unknown
}

@openEnum
@generateOptics
intEnum OpenIntEnumCollisionTest {
  ONE = 1,
  TWO = 2,
  Unknown = 3
}

@openEnum
@generateOptics
intEnum OpenIntEnumCollisionTest2 {
  ONE = 1,
  TWO = 2,
  unknown = 3
}

@openEnum
@enum([
  {value: "ONE", name: "ONE"}
])
string OpenOldEnumTest

@openEnum
@enum([
  {value: "unknown", name: "Unknown"}
])
string OpenOldEnumCollisionTest
