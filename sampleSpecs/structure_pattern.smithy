$version: "2"

namespace smithy4s.example

use alloy#structurePattern

@structurePattern(pattern: "{one}-{two}", target: TestStructurePatternTarget)
string TestStructurePattern

structure TestStructurePatternTarget {
  @required
  one: String
  @required
  two: Integer
}
