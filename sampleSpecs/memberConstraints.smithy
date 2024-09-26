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

// Regression test for https://github.com/disneystreaming/smithy4s/issues/1594
structure HasConstrainedNewtype {
    @length(min: 1)
    @required
    s: CityId
}
