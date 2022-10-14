namespace smithy4s.example.imp

use alloy#restJson
use smithy4s.example.import_test#ImportOperation
use smithy4s.example.error#NotFoundError

@restJson
service ImportService {
  version: "1.0.0",
  operations: [ImportOperation],
  errors: [NotFoundError],
}

