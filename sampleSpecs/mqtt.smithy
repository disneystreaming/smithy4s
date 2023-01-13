$version: "2"

namespace smithy4s.example

use smithy.mqtt#publish

@publish("foo")
operation PublishFoo {
    input: PublishFooInput
    output: Unit
}

@input
structure PublishFooInput {
    @required
    bar: String
}
