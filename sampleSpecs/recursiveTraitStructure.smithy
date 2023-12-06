$version: "2"

namespace smithy4s.example

@trait
structure RecursiveTraitStructure {
    @RecursiveTraitStructure
    name: String
}
