$version: "2"

namespace smithy4s.example
use smithy4s.meta#adtMember

@deprecated(message: "A compelling reason", since: "0.0.1")
structure DeprecatedStructure {
  @deprecated name: String,
  nameV2: String,
  strings: Strings
}

@deprecated
list Strings {
  member: String
}

@deprecated(message: "A compelling reason", since: "0.0.1")
union DeprecatedUnion {
  @deprecated s: String,
  s_V2: String,
  p: DeprecatedUnionProductCase,
  @deprecated
  p2: UnionProductCaseDeprecatedAtCallSite
}

@adtMember(DeprecatedUnion)
@deprecated
structure DeprecatedUnionProductCase {}

@adtMember(DeprecatedUnion)
structure UnionProductCaseDeprecatedAtCallSite {}

@deprecated
string DeprecatedString

@deprecated service DeprecatedService {
  operations: [DeprecatedOperation]
}

@deprecated
operation DeprecatedOperation {}

@deprecated
@documentation("some docs here")
enum EnumWithDeprecations {
  @deprecated
  OLD,
  NEW
}
