$version: "2.0"

namespace smithy4s.example


service NameCollision {
operations: [MyOp]
}

operation MyOp {
input: Endpoint
errors: [MyOpError]
}

@error("client")
structure MyOpError {}

structure Endpoint {

}