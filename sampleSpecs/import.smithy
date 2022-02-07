namespace smithy4s.example

use smithy4s.api#simpleRestJson
use smithy4s.example.import_test#ImportOperation

@simpleRestJson
service ImportService {
  version: "1.0.0",
  operations: [ImportOperation]
}

