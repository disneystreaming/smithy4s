$version: "2"

namespace smithy4s.example

use alloy#unknownFieldRetention

structure UnknownFieldRetentionExample {
    foo: String
    bar: String
    @unknownFieldRetention
    retainedUnknownFields: Document
}

structure DefaultUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @unknownFieldRetention
    retainedUnknownFields: Document
}

structure RequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @required
    @unknownFieldRetention
    retainedUnknownFields: Document
}

structure DefaultRequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @required
    @unknownFieldRetention
    retainedUnknownFields: Document
}
