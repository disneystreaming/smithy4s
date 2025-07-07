$version: "2.0"

namespace bar

use aws.api#data
use foo#Foo

// Checking that Foo can be found by virtue of the bar project depending on the foo project
@data("tagging")
structure Bar {
    foo: Foo
}
