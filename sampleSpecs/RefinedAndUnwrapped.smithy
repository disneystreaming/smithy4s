$version: "2"

namespace smithy4s.example


use smithy4s.meta#refinement
use smithy4s.meta#unwrap


@trait(selector: "string")
structure validM {}

@validM
@unwrap
string ByteArrayInputStream

apply validM @refinement(
    targetType: "java.io.ByteArrayInputStream"
)


structure Apple {
    data: ByteArrayInputStream
}