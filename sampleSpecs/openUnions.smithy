$version: "2"

namespace smithy4s.example

use alloy#discriminated
use alloy#jsonUnknown

union OnlyUnknownOpenUnion {
    @jsonUnknown
    unknown: Document
}

union SampleOpenUnion {
    str: String

    u: Unit

    @jsonUnknown
    unknown: Document
}

@discriminated("type")
union OnlyUnknownDiscriminatedOpenUnion {
    @jsonUnknown
    unknown: Document
}

structure StructForDiscrimination {
    @required
    str: String
}

@discriminated("type")
union SampleOpenDiscriminatedUnion {
    s: StructForDiscrimination

    u: Unit

    @jsonUnknown
    unknown: Document
}
