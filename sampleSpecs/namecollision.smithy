$version: "2.0"

namespace smithy4s.example


service NameCollision {
operations: [MyOp,Endpoint]
}

operation Endpoint {

}
operation MyOp {
errors: [MyOpError]
}

@error("client")
structure MyOpError {}