$version: "2.0"

metadata smithy4sErrorsAsScala3Unions = true

namespace hello

service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
}

operation Hello {
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
