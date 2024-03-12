$version: "2"

namespace smithy4s.example

use alloy#unknownDocumentFieldRetention
use alloy#unknownJsonFieldRetention

structure UnknownFieldRetentionExample {
    foo: String
    bar: String
    @unknownDocumentFieldRetention
    @unknownJsonFieldRetention
    retainedUnknownFields: Document
}

structure DefaultUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @unknownDocumentFieldRetention
    @unknownJsonFieldRetention
    retainedUnknownFields: Document
}

structure RequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @required
    @unknownDocumentFieldRetention
    @unknownJsonFieldRetention
    retainedUnknownFields: Document
}

structure DefaultRequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @required
    @unknownDocumentFieldRetention
    @unknownJsonFieldRetention
    retainedUnknownFields: Document
}
