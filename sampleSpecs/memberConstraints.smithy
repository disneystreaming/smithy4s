$version: "2"

namespace smithy4s.example

list ConstrainedList {
    @length(min: 1, max: 11)
    member: String
}

map ConstrainedMap {
    @length(min: 2, max: 12)
    key: String
    @length(min: 3, max: 13)
    value: String
}

