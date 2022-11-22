$version: "2.0"

namespace bar

use foo#Foo
use aws.api#data

// Checking that Foo can be found by virtue of the bar project depending on the foo project
@data("tagging")
structure Bar {
  foo: Foo
}
