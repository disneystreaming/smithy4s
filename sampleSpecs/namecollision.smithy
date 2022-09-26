$version: "2.0"

namespace smithy4s.example


service NameCollision {
operations: [MyOp]
}

operation MyOp {
errors: [MyOpError]
}

@error("client")
structure MyOpError {}