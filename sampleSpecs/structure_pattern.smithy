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

@structurePattern(pattern: "{label}:{value}", target: TestUnionPatternTarget)
string TestUnionPattern

union TestUnionPatternTarget {
    one: String
    two: Integer
}

@structurePattern(pattern: "{name}/{inner}", target: NestedStructurePatternTarget)
string NestedStructurePattern

structure NestedStructurePatternTarget {
    @required
    name: String

    @required
    inner: TestStructurePattern
}

@structurePattern(pattern: "{prefix}/{tagged}", target: NestedUnionPatternTarget)
string NestedUnionPattern

structure NestedUnionPatternTarget {
    @required
    prefix: String

    @required
    tagged: TestUnionPattern
}

@structurePattern(pattern: "{label}:{value}", target: NestedInnerUnionTarget)
string NestedInnerUnionPattern

union NestedInnerUnionTarget {
    str: String
    num: Integer
}

@structurePattern(pattern: "{id}|{choice}", target: NestedMiddlePatternTarget)
string NestedMiddlePattern

structure NestedMiddlePatternTarget {
    @required
    id: Integer

    @required
    choice: NestedInnerUnionPattern
}

@structurePattern(pattern: "{tenant}/{resource}", target: NestedTopPatternTarget)
string NestedTopPattern

structure NestedTopPatternTarget {
    @required
    tenant: String

    @required
    resource: NestedMiddlePattern
}
