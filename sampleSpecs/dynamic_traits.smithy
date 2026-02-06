$version: "2"

metadata smithy4sRenderDynamicHintNamespacePatterns = ["smithy4s.example.dynamic_traits"]

namespace smithy4s.example.dynamic_traits

@trait
structure thisWillBeDynamic {
  test: Integer
  test2: Float
}
