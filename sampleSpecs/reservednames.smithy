$version: "2.0"

namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service ReservedNameService {
    version: "1.0.0",
    operations: [Set,List,Map,Option]
}

@http(method: "POST", uri: "/api/set/", code: 204)
operation Set {
    input := {
       @required
       set:MySet
    }
}

@uniqueItems
list MySet {
    member: StringValue
}

@http(method: "POST", uri: "/api/list/{value}", code: 204)
operation List {
    input := {
        @httpLabel
        @required
        value: StringValue
    }
}
@http(method: "POST", uri: "/api/map/", code: 204)
operation Map {
    input := {
        @required
        value: myMap
    }
}

map myMap {
    key: StringKey
    value: StringValue
}

@http(method: "POST", uri: "/api/option/{value}", code: 204)
operation Option {
    input := {
        @httpLabel
        @required
        value: StringValue
    }
}

string StringKey
integer StringValue
