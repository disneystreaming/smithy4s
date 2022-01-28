namespace foo

use smithy4s.api#simpleRestJson
use smithy4s.api#discriminated

@simpleRestJson
service HelloWorldService {
  version: "0.0.1",
  errors: [GeneralServerError],
  operations: [Greet, GetIntOrString]
}

@readonly
@http(method: "GET", uri: "/hello/{name}")
operation Greet {
  input: Person,
  output: Greeting
}

@readonly
@http(method: "GET", uri: "/untagged")
operation GetIntOrString {
  output: GetIntOrStringResponse
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpHeader("X-Bamtech-Partner")
  partner: String
}

structure Greeting {
  @required
  @httpPayload
  message: String,
}

@error("server")
@httpError(500)
structure GeneralServerError {
  message: String,
}

structure GetIntOrStringResponse {
  intOrString: IntOrString,
  catOrDog: CatOrDog
}

union IntOrString {
  int: Integer,
  string: String
}

structure Cat {
  name: String
}

structure Dog {
  name: String,
  breed: String
}

@discriminated("type")
union CatOrDog {
  cat: Cat,
  dog: Dog
}


