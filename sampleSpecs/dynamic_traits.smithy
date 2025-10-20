$version: "2"

metadata smithy4sRenderDynamicHintNamespaces = ["smithy4s.example.dynamic_traits"]

namespace smithy4s.example.dynamic_traits

@trait
structure thisWillBeDynamic {
  test: Integer
}
