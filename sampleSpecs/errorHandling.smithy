$version: "2"

namespace smithy4s.example

// Where the errors are placed in this spec doesn't necessarily "make sense" but is
// mixed up to test some specific scenarios such as falling back to service-level
// errors when no operation-level ones are found.

service ErrorHandlingService {
  version: "1"
  operations: [ErrorHandlingOperation]
  errors: [EHFallbackClientError, EHServiceUnavailable]
}

operation ErrorHandlingOperation {
  input := {
    in: String
  }
  output := {
    out: String
  }
  errors: [EHNotFound, EHFallbackServerError]
}

@httpError(404)
@error("client")
structure EHNotFound {
  message: String
}

@error("client")
structure EHFallbackClientError {
  message: String
}

@httpError(503)
@error("server")
structure EHServiceUnavailable {
  message: String
}

@error("server")
structure EHFallbackServerError {
  message: String
}

service ErrorHandlingServiceExtraErrors {
  version: "1"
  operations: [ExtraErrorOperation]
  errors: [RandomOtherClientError, RandomOtherServerError, RandomOtherClientErrorWithCode, RandomOtherServerErrorWithCode]
}

operation ExtraErrorOperation {
  input := {
    in: String
  }
}

@error("client")
structure RandomOtherClientError {
  message: String
}

@error("server")
structure RandomOtherServerError {
  message: String
}

@httpError(404)
@error("client")
structure RandomOtherClientErrorWithCode {
  message: String
}

@httpError(503)
@error("server")
structure RandomOtherServerErrorWithCode {
  message: String
}
