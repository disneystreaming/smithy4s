$version: "2"

namespace smithy4s.example

use smithy4s.meta#typeclass

// NOTE: normally you would likely not need to add instances of eq or other typeclasses
// to all of your types. Here I am doing it just to test different rendering cases.
// In reality, you don't need an `eq` instance for the members of a struct in order
// to have an `eq` instance for the struct itself. This is because the interpreter provided
// will use the schema to derive the instance rather than delegating to the instances on
// the target types of the members directly.

@trait
@typeclass(targetType: "cats.Eq", interpreter: "smithy4s.example.typeclass.EqInterpreter")
structure eq {}

@eq
structure MovieTheater {
  name: String
}

@eq
union PersonContactInfo {
  email: PersonEmail
  phone: PersonPhoneNumber
}

@eq
enum NetworkConnectionType {
  ETHERNET
  WIFI
}

@eq
string PersonEmail

@eq
string PersonPhoneNumber
