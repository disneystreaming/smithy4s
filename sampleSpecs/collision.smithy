$version: "2"

namespace smithy4s.example.collision

// regression test for https://github.com/disneystreaming/smithy4s/issues/1788
@smithy4s.meta#adt
union UnionWithCollision {
    s: Struct
}

// The name matters, as it conflicts with Schema.struct
structure Struct {
    name: String
}

service CollisionService {
    operations: [
        AlgParameterOperation
    ]
}

operation AlgParameterOperation {
    input := {
        @required
        alg: String
    }
}
