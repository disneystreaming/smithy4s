$version: "2"

namespace smithy4s.example

use alloy#nullable
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


@error("client")
structure ErrorRequiredMessage {
  @required
  @errorMessage
  message: String
}

@error("client")
structure ErrorNullableMessage {
  @nullable
  @errorMessage
  message: String
}

@error("server")
structure ErrorNullableRequiredMessage {
  @nullable
  @errorMessage
  @required
  message: String
}

string CustomErrorMessageType

@error("server")
structure ErrorCustomTypeMessage {
  @errorMessage
  message: CustomErrorMessageType
}

@error("server")
structure ErrorCustomTypeRequiredMessage {
  @errorMessage
  @required
  message: CustomErrorMessageType
}

@error("server")
structure ErrorNullableCustomTypeMessage {
  @nullable
  @errorMessage
  message: CustomErrorMessageType
}

@error("server")
structure ErrorNullableCustomTypeRequiredMessage {
  @nullable
  @errorMessage
  @required
  message: CustomErrorMessageType
}
