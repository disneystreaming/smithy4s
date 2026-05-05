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
        GetPayloadItem
        GetHeaderItem
        GetStatusCode
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

@unpackedOutput
@readonly
@http(method: "GET", uri: "/payload", code: 200)
operation GetPayloadItem {
    output := {
        @required
        @httpPayload
        item: UnpackedItem
    }
}

@unpackedOutput
@readonly
@http(method: "GET", uri: "/header", code: 200)
operation GetHeaderItem {
    output := {
        @required
        @httpHeader("X-Item-Id")
        itemId: String
    }
}

@unpackedOutput
@readonly
@http(method: "GET", uri: "/status", code: 200)
operation GetStatusCode {
    output := {
        @required
        @httpResponseCode
        code: Integer
    }
}

structure UnpackedItem {
    @required
    id: String
}
