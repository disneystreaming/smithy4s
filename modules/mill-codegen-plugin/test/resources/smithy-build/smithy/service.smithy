$version: "2.0"

namespace service

use alloy#simpleRestJson
use smithy.api#httpHeader

@simpleRestJson
service Foo {
    version: "1.0.0",
    operations: [Bar]
}

operation Bar {
    input: Input,
    output: Output,
}

structure Input {
    @httpHeader("X-Foo")
    foo: String
}

structure Output {
    @httpHeader("X-Foo")
    foo: String,
    @httpPayload
    bar: Integer,
}
