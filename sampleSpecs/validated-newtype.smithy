$version: "2"

namespace smithy4s.example

use smithy4s.meta#unwrap

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
string ValidatedString

structure ValidatedFoo {
    name: ValidatedString
}

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
@unwrap
string UnwrappedValidatedString

structure UnwrappedValidatedFoo {
    name: UnwrappedValidatedString
}