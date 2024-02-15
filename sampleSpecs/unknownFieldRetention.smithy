$version: "2"

namespace smithy4s.example

use alloy#unknownFieldRetention

structure UnknownFieldRetentionExample {
    foo: String
    bar: String
    @unknownFieldRetention
    bazes: RetainedUnknownFields
 }

structure DefaultUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @unknownFieldRetention
    bazes: RetainedUnknownFields
 }

structure RequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @required
    @unknownFieldRetention
    bazes: RetainedUnknownFields
 }

structure DefaultRequiredUnknownFieldRetentionExample {
    foo: String
    bar: String
    @default
    @required
    @unknownFieldRetention
    bazes: RetainedUnknownFields
 }
 
 map RetainedUnknownFields {
    key: String
    value: Document 
}
