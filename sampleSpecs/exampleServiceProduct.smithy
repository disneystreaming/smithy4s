$version: "2"

namespace smithy4s.example.product

use alloy#simpleRestJson
use smithy4s.example#PutObject
use smithy4s.meta#generateServiceProduct

@simpleRestJson
@generateServiceProduct
service ObjectService {
    version: "1.0.0"
    operations: [PutObject]
}
