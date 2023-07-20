$version: "2"

namespace smithy4s.example

use smithy4s.meta#generateOptics

@generateOptics
union OpticsUnion {
  one: OpticsStructure
}

@generateOptics
structure OpticsStructure {
  two: OpticsEnum
}

@generateOptics
enum OpticsEnum {
  A, B
}
