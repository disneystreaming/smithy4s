$version: "2"

namespace smithy4s.example.hello

use alloy#simpleRestJson

@simpleRestJson
@tags(["testServiceTag"])
service HelloWorldService {
  version: "1.0.0",
  // Indicates that all operations in `HelloWorldService`,
  // here limited to Hello, can return `GenericServerError`.
  errors: [GenericServerError, SpecificServerError],
  operations: [Hello]
}

@error("server")
@httpError(500)
structure GenericServerError {
  message: String
}

@error("server")
@httpError(599)
structure SpecificServerError {
  message: String
}

@http(method: "POST", uri: "/{name}", code: 200)
@tags(["testOperationTag"])
operation Hello {
  input: Person,
  output: Greeting
}


structure Person {
  @httpLabel
  @required
  name: String,

  @httpQuery("town")
  town: String
}

structure Greeting {
  @required
  message: String
}
