namespace smithy4s.example

use smithy4s.meta#unwrap

@length(min: 1, max: 10)
string SimpleValidatedString

@unwrap
@length(min: 1, max: 10)
string UnwrappedValidatedString

structure TestValidatedNewTypes {
  one: SimpleValidatedString
  two: UnwrappedValidatedString
}
