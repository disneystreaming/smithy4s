$version: "2.0"

namespace baz

use foo#Foo

// Checking that Foo can be found by virtue of the upstream `bar` project
// defined as a compile-scope library dependency was published with an indication
// in the manifest that it used the `foo` project for code generation.
structure Baz {
  foo: Foo
}
