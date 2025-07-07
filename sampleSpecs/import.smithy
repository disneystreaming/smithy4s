$version: "2"

namespace smithy4s.example.imp

use alloy#simpleRestJson
use smithy4s.example.error#NotFoundError
use smithy4s.example.import_test#ImportOperation

@simpleRestJson
service ImportService {
    version: "1.0.0"
    operations: [
        ImportOperation
    ]
    errors: [
        NotFoundError
    ]
}
