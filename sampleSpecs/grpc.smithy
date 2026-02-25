$version: "2"

namespace smithy4s.example.grpc

use alloy.proto#grpc
use alloy.proto#grpcError

@grpc
service GrpcGreetingService {
    operations: [Greet]
}

operation Greet {
    input: GreetInput
    output: GreetOutput
    errors: [NotFoundError, PermissionDeniedError]
}

structure GreetInput {
    @required
    name: String
}

structure GreetOutput {
    @required
    greeting: String
}

@error("client")
@grpcError(code: 5, message: "Resource not found")
structure NotFoundError {
    @required
    message: String
}

@error("client")
@grpcError(code: 7, message: "Permission denied")
structure PermissionDeniedError {
    @required
    message: String
}
