namespace smithy4s.example

use smithy4s.api#untagged
use smithy4s.meta#vector
use smithy4s.meta#indexedSeq

service EmptyService {
  version: "1.0"
}

structure BigStruct{
  @required
  a1: Integer,
  @required
  a2: Integer,
  @required
  a3: Integer,
  @required
  a4: Integer,
  @required
  a5: Integer,
  @required
  a6: Integer,
  @required
  a7: Integer,
  @required
  a8: Integer,
  @required
  a9: Integer,
  @required
  a10: Integer,
  @required
  a11: Integer,
  @required
  a12: Integer,
  @required
  a13: Integer,
  @required
  a14: Integer,
  @required
  a15: Integer,
  @required
  a16: Integer,
  @required
  a17: Integer,
  @required
  a18: Integer,
  @required
  a19: Integer,
  @required
  a20: Integer,
  @required
  a21: Integer,
  @required
  a22: Integer,
  @required
  a23: Integer
}

@enum([
  {value: "foo:foo:foo"},
  {value: "bar:bar:bar"},
  {value: "_"},
])
string EnumWithSymbols


union CheckedOrUnchecked {
  @pattern("^\\w+$")
  checked: String,
  raw: String
}

@untagged
union CheckedOrUnchecked2 {
  @pattern("^\\w+$")
  checked: String,
  raw: String
}

@trait
structure SomeCollections {
  @required
  someList: SomeList,
  @required
  someSet: SomeSet,
  @required
  someMap : SomeMap
}

list SomeList {
  member: String
}

set SomeSet {
  member: String
}

map SomeMap {
  key: String,
  value: String
}

@SomeCollections(someList: ["a"], someSet: ["b"], someMap: {"a" : "b"})
integer SomeInt

@suppress(["UnreferencedShape"])
structure RangeCheck {
  @required
  @range(min: 1)
  qty: Integer
}
