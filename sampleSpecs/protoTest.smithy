$version: "2"

namespace smithy4s.example

use alloy#UUID
use alloy#openEnum
use alloy.openapi#openapiExtensions
use alloy.proto#protoEnabled
use alloy.proto#protoIndex
use alloy.proto#protoInlinedOneOf
use alloy.proto#protoNumType
use alloy.proto#protoWrapped

@protoEnabled
structure Integers {
    @protoIndex(1)
    @required
    int: Integer
    @protoIndex(2)
    @protoNumType("SIGNED")
    @required
    sint: Integer
    @protoIndex(3)
    @protoNumType("UNSIGNED")
    @required
    uint: Integer
    @protoIndex(4)
    @protoNumType("FIXED")
    @required
    fixedUint: Integer
    @protoIndex(5)
    @protoNumType("FIXED_SIGNED")
    @required
    fixedSint: Integer
}

@protoEnabled
structure Longs {
    @protoIndex(1)
    @required
    long: Long
    @protoIndex(2)
    @protoNumType("SIGNED")
    @required
    slong: Long
    @protoIndex(3)
    @protoNumType("UNSIGNED")
    @required
    ulong: Long
    @protoIndex(4)
    @protoNumType("FIXED")
    @required
    fixedLong: Long
    @protoIndex(5)
    @protoNumType("FIXED_SIGNED")
    @required
    fixedSlong: Long
}

@protoEnabled
structure OtherScalars {
    @protoIndex(1)
    @required
    boolean: Boolean
    @required
    @protoIndex(2)
    byte: Byte
    @required
    @protoIndex(3)
    float: Float
    @required
    @protoIndex(4)
    double: Double
    @required
    @protoIndex(5)
    short: Short
}

@protoEnabled
structure WrappedScalars {
    @protoIndex(1)
    @protoWrapped
    int: Integer
    @protoIndex(2)
    @protoWrapped
    bool: Boolean
}

@protoEnabled
structure StringWrapper {
    @protoIndex(1)
    @required
    string: String
}

@protoEnabled
structure OptionalStringWrapper {
    @protoIndex(1)
    string: String
}

@protoEnabled
structure BigDecimalWrapper {
    @protoIndex(1)
    @required
    bigDecimal: BigDecimal
}

@protoEnabled
structure Other {
    @protoIndex(3)
    @required
    bigDecimal: BigDecimal
    @required
    @protoIndex(4)
    bigInteger: BigInteger
    @protoIndex(5)
    @required
    uuid: UUID
}

@protoEnabled
structure MessageWrapper {
    @protoIndex(1)
    @required
    message: Integers
}

@protoEnabled
structure OptionalMessageWrapper {
    @protoIndex(1)
    message: Integers
}

@protoEnabled
structure IntListWrapper {
    @protoIndex(1)
    @required
    ints: IntList
}

list StringList {
    member: String
}

@protoWrapped
list WrappedStringList {
    member: String
}

@protoEnabled
structure StringListWrapper {
    @protoIndex(1)
    @required
    strings: StringList
    @protoIndex(2)
    @required
    wrappedStrings: WrappedStringList
}

integer MyInt

list MyIntList {
    member: MyInt
}

@protoEnabled
structure MyIntListWrapper {
    @protoIndex(1)
    @required
    ints: MyIntList
}

@protoEnabled
structure Recursive {
    @protoIndex(1)
    recursive: Recursive
}

@protoEnabled
structure UnionWrapper {
    @protoIndex(1)
    myUnion: MyUnion
}

union MyUnion {
    @protoIndex(1)
    int: Integer
    @protoIndex(2)
    bool: Boolean
}

@protoEnabled
structure InlinedUnionWrapper {
    myInlinedUnion: MyInlinedUnion
}

@protoInlinedOneOf
union MyInlinedUnion {
    @protoIndex(1)
    int: Integer
    @protoIndex(2)
    bool: Boolean
}

@protoEnabled
structure StringMapWrapper {
    @protoIndex(1)
    @required
    values: StringMap
}
