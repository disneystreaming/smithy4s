$version: "2.0"

namespace smithy4s.example.collision

use alloy#restJson

@restJson
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
    member: String
}

@http(method: "POST", uri: "/api/list/", code: 204)
operation List {
    input := {
        @required
        list: MyList
    }
}

list MyList {
    member: String
}

@http(method: "POST", uri: "/api/map/", code: 204)
operation Map {
    input := {
        @required
        value: MyMap
    }
}

map MyMap {
    key: String
    value: String
}

@http(method: "POST", uri: "/api/option/", code: 204)
operation Option {
    input := {
        value: String
    }
}

string String
