$version: "2.0"

namespace bar

use foo#Foo

// Checking that Foo can be found by virtue of the bar project depending on the foo project
structure Bar {
  foo: Foo
}
