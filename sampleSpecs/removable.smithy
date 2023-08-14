$version: "2"

namespace smithy4s.example

use alloy#nullable

structure Patchable {
    @required
    a: Integer
    b: Integer
    @nullable
    c: Integer
}
