$version: "2"

namespace smithy4s.example

use alloy#nullable

// The @nullable trait allows distinguishing between explicit null and absence of a value
structure Patchable {
    @nullable
    allowExplicitNull: Integer
}

// Edge case handling - most of these combinations are not very sensible and should probably not be used
// but are described here so we can test they're handled correctly
structure PatchableEdgeCases {
    @nullable
    @required
    required: Integer
    @nullable
    @required
    requiredDefaultValue: Integer = 3
    @nullable
    @required
    @default
    requiredDefaultNull: Integer
    @nullable
    defaultValue: Integer = 5
    @nullable
    @default
    defaultNull : Integer
}