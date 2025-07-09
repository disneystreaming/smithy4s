$version: "2.0"

namespace smithy4s.example.bincompat

use alloy#nullable
use smithy4s.meta#bincompatAdded
use smithy4s.meta#bincompatFriendly

@bincompatFriendly
structure BincompatStructSmall {
    @required
    base1: String

    @bincompatAdded(version: "2.0")
    added2_1: String
}

@bincompatFriendly
structure BincompatStruct {
    @required
    base1: String

    @required
    base2: String

    base3: String

    @bincompatAdded(version: "2.0")
    @required
    added2_1: String = "woop2_1"

    @bincompatAdded(version: "2.0")
    added2_2: String

    @bincompatAdded(version: "3.0")
    @required
    added3_1: String = "woop3_1"

    @bincompatAdded(version: "3.0")
    added3_2: String

    @bincompatAdded(version: "4.0")
    added4_1: String = "woop4_1"

    @bincompatAdded(version: "4.0")
    @nullable
    added4_2: String

    // intentionally put last
    @required
    base4: String
}

@bincompatFriendly
structure BincompatEmptyStruct {}

@bincompatFriendly
structure BincompatOneFieldStruct {
    s: String
}

@bincompatFriendly
union BincompatTinyUnion {
    s1: BincompatEmptyStruct
}

@bincompatFriendly
union BincompatUnion {
    s1: BincompatEmptyStruct
    s2: BincompatEmptyStruct
}

// N.B. there's no good way to prove bincompat-ness of traits with just codegen
// but this is here regardless, to showcase that the use-site compiles _at least on the updated version_.
@bincompatFriendly
@trait
structure BincompatFriendlyTraitStruct {
    @required
    base1: String

    @required
    base2: String

    base3: String

    @bincompatAdded(version: "2.0")
    added2_1: String = "woop2_1"

    @bincompatAdded(version: "3.0")
    @required
    added3_1: String = "woop3_1"
}

@BincompatFriendlyTraitStruct(base1: "b1", base2: "b2", added3_1: "b4")
structure HasBincompatTrait {}

@bincompatFriendly
enum BincompatEnum {
    A
    B
    C
}

@bincompatFriendly
intEnum BincompatIntEnum {
    A = 1
    B = 2
    C = 3
}

@bincompatFriendly
@alloy#openEnum
enum BincompatOpenEnum {
    A
    B
    C
}
