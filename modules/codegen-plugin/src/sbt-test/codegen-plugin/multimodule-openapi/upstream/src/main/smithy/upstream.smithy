$version: "2"

namespace upstream

use alloy#simpleRestJson

@simpleRestJson
service UpstreamService {
    version: "1.0.0"
    operations: [
        GetGreeting
    ]
}

@readonly
@http(method: "GET", uri: "/greeting", code: 200)
operation GetGreeting {
    output := {
        @required
        greeting: Greeting
    }
}

structure Greeting {
    @required
    message: String
}
