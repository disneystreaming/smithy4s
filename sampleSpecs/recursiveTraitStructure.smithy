$version: "2"

namespace smithy4s.example

// https://github.com/disneystreaming/smithy4s/issues/1296
@trait
structure RecursiveTraitStructure {
    @RecursiveTraitStructure
    name: String
}

// https://github.com/disneystreaming/smithy4s/issues/1308
@trait
list RecursiveListTrait {
    @RecursiveListTrait
    member: String
}

// https://github.com/disneystreaming/smithy4s/issues/1308
@trait
map RecursiveMapTrait {
    @RecursiveMapTrait
    key: String

    value: String
}
