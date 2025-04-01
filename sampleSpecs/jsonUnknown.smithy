$version: "2"

namespace smithy4s.example

use alloy#jsonUnknown

structure JsonUnknownExample {
  s: String
  i: Integer
  @jsonUnknown
  additionalProperties: AdditionalProperties
}

map AdditionalProperties {
  key: String
  value: Document
}
