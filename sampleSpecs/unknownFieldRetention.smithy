$version: "2"

namespace smithy4s.example

use alloy#unknownDocumentFieldRetention

structure UnknownFieldRetentionExample {
    foo: String
    bar: String
    @unknownDocumentFieldRetention
    retainedUnknownFields: Document
}

structure DefaultUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @unknownDocumentFieldRetention
    retainedUnknownFields: Document
}

structure RequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @required
    @unknownDocumentFieldRetention
    retainedUnknownFields: Document
}

structure DefaultRequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @required
    @unknownDocumentFieldRetention
    retainedUnknownFields: Document
}
