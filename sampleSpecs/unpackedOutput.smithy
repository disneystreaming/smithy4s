$version: "2.0"

namespace smithy4s.example

use alloy#simpleRestJson
use smithy4s.meta#unpackedOutput

@simpleRestJson
service UnpackedOutputService {
    version: "1.0.0"
    operations: [
        GetRequiredItem
        GetOptionalItem
    ]
}

@unpackedOutput
@readonly
@http(method: "GET", uri: "/required", code: 200)
operation GetRequiredItem {
    output := {
        @required
        item: UnpackedItem
    }
}

@unpackedOutput
@readonly
@http(method: "GET", uri: "/optional", code: 200)
operation GetOptionalItem {
    output := {
        item: UnpackedItem
    }
}

structure UnpackedItem {
    @required
    id: String
}
