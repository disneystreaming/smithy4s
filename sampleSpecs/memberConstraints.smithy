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
structure HasConstrainedNewtypes {
    // string newtype
    @length(min: 1)
    @required
    a: BucketName
    // string newtype, double-constrained
    @length(min: 1)
    @required
    b: CityId
    // int newtype
    @range(min: 1)
    c: ObjectSize
    // list newtype, oh wait these are just lists. Still.
    @length(min: 1)
    d: SomeIndexSeq
    // blob newtype
    @length(min: 1)
    e: PNG
}
