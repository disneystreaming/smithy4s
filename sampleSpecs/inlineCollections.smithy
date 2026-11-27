$version: "2.1"

namespace smithy4s.example

structure InlineCollections {
    strings: [String]

    @required
    requiredStrings: [String] = []

    structs: [InlineCollectionsItem]

    nested: [[Integer]]

    stringToInt: {String: Integer}

    stringToStructs: {String: [InlineCollectionsItem]}

    ref: StringList
}

structure InlineCollectionsItem {
    name: String
}

// smithy.api#discriminated, with the default discriminator field (`__type`)
@smithy.api#discriminated
union SmithyDiscriminatedDefault {
    one: InlineCollectionsItem
    two: InlineCollections
}

// smithy.api#discriminated, with an explicit discriminator field
@smithy.api#discriminated("kind")
union SmithyDiscriminated {
    one: InlineCollectionsItem
    two: InlineCollections
}

// when both traits are present, alloy#discriminated takes precedence
@smithy.api#discriminated("kind")
@alloy#discriminated("type")
union BothDiscriminated {
    one: InlineCollectionsItem
    two: InlineCollections
}
