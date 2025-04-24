$version: "2"

namespace foo

use alloy.proto#protoEnabled

@protoEnabled
structure Foo {
    a: String
    b: Integer
}
