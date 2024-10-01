$version: "2"

namespace smithy4s.example

use smithy4s.meta#validateNewtype

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
@validateNewtype
string ValidatedString

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
string NonValidatedString

structure ValidatedFoo {
    name: ValidatedString = "abc"
}

