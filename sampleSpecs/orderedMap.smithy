$version: "1"

namespace smithy4s.example

// TODO: since @preserveKeyOrder changes the underlying type depending on scala version, the bootstrap module codegen task
// should generate separate srcX_xx directories to render these differences
// @alloy#preserveKeyOrder
map OrderedMap {
    key: String
    value: String
}
