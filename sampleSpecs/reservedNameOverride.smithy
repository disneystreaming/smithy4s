$version: "2.0"

namespace smithy4s.example.reservedNameOverride

use smithy4s.api#simpleRestJson

@simpleRestJson
service ReservedNameOverrideService {
    version: "1.0.0",
    operations: [SetOp]
}

@http(method: "POST", uri: "/api/set/", code: 204)
operation SetOp {
    input := {
       @required
       set: Set
    }
}

structure Set {
    @required
    someField: String,
    @required
    otherField: Integer
}


