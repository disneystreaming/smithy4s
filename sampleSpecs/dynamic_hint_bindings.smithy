$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#renderAsDynamicBinding
use smithy4s.example.dynamic_traits#thisWillBeDynamic

@trait
@renderAsDynamicBinding
structure testDynamicBinding {
    str: String
    int: Integer
}

@testDynamicBinding(str: "test")
@thisWillBeDynamic(test: 101, test2: 123.456)
@since("1")
structure ShouldHaveDynamicBinding {
    @testDynamicBinding(str: "test2", int: 1234)
    @since("2")
    a: String

    @testDynamicBinding
    @length(min: 1)
    b: String
}

// Since this has no static bindings, it should not use
// `.lazily`
@testDynamicBinding(str: "test")
structure ShouldHaveDynamicBindingTwo {}

