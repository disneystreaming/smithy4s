$version: "2.0"

namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service ReservedNameService {
    version: "1.0.0",
    operations: [Set,List,Option]
}

@http(method: "POST", uri: "/api/set/{key}", code: 204)
operation Set {
    input := {
        @httpLabel
        @required
        key: Key,

        value: Value
    }
}

@http(method: "POST", uri: "/api/list/{value}", code: 204)
operation List {
    input := {
        @httpLabel
        @required
        value: Value
    }
}

@http(method: "POST", uri: "/api/option/{value}", code: 204)
operation Option {
    input := {
        @httpLabel
        @required
        value: Value
    }
}



string Key
integer Value