namespace smithy4s.example.imp

use smithy4s.api#simpleRestJson
use smithy4s.example.import_test#ImportOperation
use smithy4s.example.error#NotFoundError

@simpleRestJson
service ImportService {
  version: "1.0.0",
  operations: [ImportOperation],
  errors: [NotFoundError],
}

