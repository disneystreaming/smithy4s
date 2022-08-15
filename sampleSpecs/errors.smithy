namespace smithy4s.example

use smithy4s.meta#errorMessage

@error("server")
@httpError(507)
structure NoMoreSpace {
  @required
  message: String,
  foo: Foo
}

@error("server")
structure ServerErrorCustomMessage {
  @errorMessage
  messageField: String
}
