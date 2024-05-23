$version: "2"

metadata "proto_options" = [{

}]
namespace smithy4s.example.protobuf

use alloy#UUID
use alloy#openEnum
use alloy#uuidFormat
use alloy.openapi#openapiExtensions
use alloy.proto#protoCompactUUID
use alloy.proto#protoEnabled
use alloy.proto#protoIndex
use alloy.proto#protoInlinedOneOf
use alloy.proto#protoNumType
use alloy.proto#protoWrapped

@protoEnabled
structure Integers {
    @required
    int: Integer
    @protoNumType("SIGNED")
    @required
    sint: Integer
    @protoNumType("UNSIGNED")
    @required
    uint: Integer
    @protoNumType("FIXED")
    @required
    fixedUint: Integer
    @protoNumType("FIXED_SIGNED")
    @required
    fixedSint: Integer
}

@protoEnabled
structure Longs {
    @required
    long: Long
    @protoNumType("SIGNED")
    @required
    slong: Long
    @protoNumType("UNSIGNED")
    @required
    ulong: Long
    @protoNumType("FIXED")
    @required
    fixedLong: Long
    @protoNumType("FIXED_SIGNED")
    @required
    fixedSlong: Long
}

@protoEnabled
structure OtherScalars {
    @required
    boolean: Boolean
    @required
    byte: Byte
    @required
    float: Float
    @required
    double: Double
    @required
    short: Short
}

@protoEnabled
structure WrappedScalars {
    @protoWrapped
    int: Integer
    @protoWrapped
    bool: Boolean
}

@protoEnabled
structure StringWrapper {
    @required
    string: String
}

@protoEnabled
structure OptionalStringWrapper {
    string: String
}

@protoEnabled
structure BigDecimalWrapper {
    @required
    bigDecimal: BigDecimal
}

@uuidFormat
@protoCompactUUID
string CompactUUID

@protoEnabled
structure UUIDWrapper {
    uuid: UUID
    compactUUID: CompactUUID
}

list IntList {
    member: Integer
}

@protoEnabled
structure MessageWrapper {
    @required
    message: Integers
}

@protoEnabled
structure OptionalMessageWrapper {
    message: Integers
}

@protoEnabled
structure IntListWrapper {
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
    @required
    strings: StringList
    @required
    wrappedStrings: WrappedStringList
}

integer MyInt

list MyIntList {
    member: MyInt
}

@protoEnabled
structure MyIntListWrapper {
    @required
    ints: MyIntList
}

@protoEnabled
structure Recursive {
    recursive: Recursive
}

@protoEnabled
structure UnionWrapper {
    myUnion: MyUnion
}

union MyUnion {
    int: Integer
    bool: Boolean
    @protoWrapped
    list: MyIntList
    @protoWrapped
    map: StringMap
}

@protoEnabled
structure InlinedUnionWrapper {
    myInlinedUnion: MyInlinedUnion
}

@protoInlinedOneOf
union MyInlinedUnion {
    int: Integer
    bool: Boolean
}

@protoEnabled
structure StringMapWrapper {
    @required
    values: StringMap
}

map StringMap {
    key: String
    value: Integer
}

@protoEnabled
structure Enums {
    @required
    closedString: ClosedString
    @required
    openString: OpenString
    @required
    closedInt: ClosedInt
    @required
    openInt: OpenInt
}

enum ClosedString {
    FOO
    BAR
}

@openEnum
enum OpenString {
    FOO
    BAR
}

intEnum ClosedInt {
    @protoIndex(0)
    FOO = 0
    @protoIndex(1)
    BAR = 1
}

@openEnum
intEnum OpenInt {
    FOO = 0
    BAR = 1
}

@protoEnabled
structure RefinedIntWrapped {
    @required
    @range(min: 1, max: 10)
    int: Integer
}

@protoEnabled
structure StructureWithCustomIndexes {
    @protoIndex(4)
    a: Integer
    @protoIndex(3)
    b: Integer = 0
    @protoIndex(2)
    @required
    c: Integer
    @protoIndex(1)
    d: UnionWithCustomIndexes
}

union UnionWithCustomIndexes {
    @protoIndex(3)
    a: Integer
    @protoIndex(2)
    b: Integer
    @protoIndex(1)
    c: Integer
}
