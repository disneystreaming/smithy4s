$version: "2"

namespace downstream

use alloy#simpleRestJson
use upstream#Greeting

@simpleRestJson
service DownstreamService {
    version: "1.0.0"
    operations: [
        Greet
    ]
}

@readonly
@http(method: "GET", uri: "/greet", code: 200)
operation Greet {
    output := {
        @required
        greeting: Greeting
    }
}
