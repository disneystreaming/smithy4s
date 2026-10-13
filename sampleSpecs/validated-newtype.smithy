$version: "2"

namespace smithy4s.example

use smithy4s.meta#validateNewtype
use smithy4s.meta#scalaImports

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
@validateNewtype
string ValidatedString

@pattern("[a-zA-Z0-9]+")
@validateNewtype
string AccountId

@validateNewtype
@length(min: 1)
string DeviceId

@length(min: 1)
@pattern("[a-zA-Z0-9]+")
string NonValidatedString

structure ValidatedFoo {
    name: ValidatedString = "abc"
}

@length(max: 1)
@validateNewtype
list ValidatedConstrainedList {
    member: String
}

@validateNewtype
@uniqueItems
list ValidatedSetConstrainedMember {
    @length(max: 2)
    member: String
}

@validateNewtype
@length(max: 1)
@smithy4s.meta#indexedSeq
list ValidatedConstrainedIndexedSeqConstrainedMember {
    @length(max: 2)
    member: String
}

@validateNewtype
@length(max: 1)
list ValidatedConstrainedListRefinedMember {
    member: Name
}

@validateNewtype
@length(max: 1)
@scalaImports(["smithy4s.example.instances._"])
@smithy4s.meta#vector
list ValidatedConstrainedVectorRefinedConstrainedMember {
    @length(max:2)
    member: Name
}

@nonEmptyListFormat
@validateNewtype
list ValidatedRefinedList {
    member: String
}

@nonEmptyListFormat
@validateNewtype
list ValidatedRefinedListConstrainedMember {
    @length(max: 2)
    member: String
}

@length(max: 1)
@validateNewtype
map ValidatedConstrainedMap {
    key: String
    value: Integer
}

@validateNewtype
map ValidatedMapConstrainedKey {
    @length(max: 2)
    key: String
    value: Integer
}

@validateNewtype
map ValidatedMapConstrainedValue {
    key: String
    @pattern("^[a-zA-Z0-9]+$")
    value: String
}

// @nonEmptyMapFormat
// @validateNewtype
// map ValidatedRefinedMap {
//     key: String
//     value: Integer
// }