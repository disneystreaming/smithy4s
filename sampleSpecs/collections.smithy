$version: "2"

namespace smithy4s.example

list ListWithMemberHints {
    @documentation("listFoo")
    member: String
}

map MapWithMemberHints {
    @documentation("mapFoo")
    key: String
    @documentation("mapBar")
    @deprecated
    value: Integer
}

list StringList {
    member: String
}

@uniqueItems
list StringSet {
    member: String
}

map StringMap {
    key: String
    value: String
}
