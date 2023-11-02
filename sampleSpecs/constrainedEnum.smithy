$version: "2"

namespace smithy4s.example

// see https://github.com/disneystreaming/smithy4s/issues/1282
// We're testing that the render code compiles correctly, which
// depends on the presence of a RefinementProvider between Range
// and an enumeration value.
structure StructureConstrainingEnum {
    @length(min: 2)
    @pattern("$aaa$")
    letter: Letters
    @range(max: 1)
    card: FaceCard
}
