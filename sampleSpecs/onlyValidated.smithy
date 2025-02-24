$version: "2"

namespace smithy4s.example.onlyValidated

use smithy4s.meta#validateNewtype

// DO NOT add any other shapes in this namespace!
// This serves as a regression test for https://github.com/disneystreaming/smithy4s/issues/1655.
@validateNewtype
@length(min: 1)
string SomeValidatedNewtype
