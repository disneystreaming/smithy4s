$version: "2"

namespace smithy4s.example

use smithy4s.meta#typeclass

// NOTE: normally you would likely not need to add instances of hash or other typeclasses
// to all of your types. Here I am doing it just to test different rendering cases.
// In reality, you don't need an `hash` instance for the members of a struct in order
// to have an `hash` instance for the struct itself. This is because the interpreter provided
// will use the schema to derive the instance rather than delegating to the instances on
// the target types of the members directly.

@trait
@typeclass(targetType: "cats.Hash", interpreter: "smithy4s.interopcats.SchemaVisitorHash")
structure hash {}

@hash
structure MovieTheater {
  name: String
}

@hash
union PersonContactInfo {
  email: PersonEmail
  phone: PersonPhoneNumber
}

@hash
enum NetworkConnectionType {
  ETHERNET
  WIFI
}

@hash
string PersonEmail

@hash
string PersonPhoneNumber
