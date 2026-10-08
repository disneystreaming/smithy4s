$version: "2"

namespace foo

use alloy#simpleRestJson

@simpleRestJson
service HelloService {
    version: "1.0.0"
    operations: [
        Hello
    ]
}

@readonly
@http(method: "GET", uri: "/hello", code: 200)
operation Hello {
    output := {
        @required
        message: String
    }
}
