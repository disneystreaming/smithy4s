$version: "2"

namespace smithy4s.example

use smithy4s.meta#typeclass

@trait
@typeclass(targetType: "cats.Eq", interpreter: "smithy4s.example.typeclass.EqInterpreter")
structure eq {}

@eq
structure MovieTheater {
  name: String
}
