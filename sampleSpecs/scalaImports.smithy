$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#scalaImports

@scalaImports(["smithy4s.refined.Age.provider._"])
structure StructureWithScalaImports {
  @range(min: 13, max: 19)
  teenage: Age
}
