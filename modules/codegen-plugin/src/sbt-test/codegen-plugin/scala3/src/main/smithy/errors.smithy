$version: "2.0"

metadata smithy4sErrorsAsScala3Unions = true

namespace smithy4s.errors

service ErrorService {
  version: "1.0.0",
  operations: [ErrorOp]
}

operation ErrorOp {
  input: Unit,
  output: Unit,
  errors: [BadRequest, InternalServerError]
}

@error("client")
structure BadRequest {
  @required
  reason: String
}

@error("server")
structure InternalServerError {
  @required
  stackTrace: String
}
