namespace smithy4s.example

list ListWithMemberHints {
  @documentation("listFoo")
  member: String
}

map MapWithMemberHints {
  @documentation("mapFoo")
  key: String

  @documentation("mapBar")
  @deprecated
  value: Integer
}
