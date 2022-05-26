namespace foo

use smithy4s.api#simpleRestJson
use smithy4s.api#discriminated
use smithy4s.api#untagged

@simpleRestJson
service HelloWorldService {
  version: "0.0.1",
  errors: [GeneralServerError],
  operations: [Greet, GetUnion, GetValues]
}

@readonly
@http(method: "GET", uri: "/hello/{name}/{ts}")
operation Greet {
  input: Person,
  output: Greeting
}

@readonly
@http(method: "GET", uri: "/default")
operation GetUnion {
  output: GetUnionResponse
}

@readonly
@http(method: "GET", uri: "/values")
operation GetValues {
  output: ValuesResponse
}

structure Person {
  @httpLabel
  @required
  name: String,

  @httpHeader("X-Bamtech-Partner")
  partner: String,

  @httpHeader("when")
  when: Timestamp,

@httpQuery("from")
  from: Timestamp,

  @httpLabel
  @required
  ts: Timestamp
}

structure Greeting {
  @required
  @httpPayload
  message: String
}

@error("server")
@httpError(500)
structure GeneralServerError {
  message: String,
}

structure GetUnionResponse {
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

structure ValuesResponse {
  values: Values
}

list Values {
  member: SomeValue
}

@untagged
union SomeValue {
  message: String,
  value: Integer
}

@discriminated("type")
union CatOrDog {
  cat: Cat,
  dog: Dog
}


