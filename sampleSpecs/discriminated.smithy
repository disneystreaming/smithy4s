namespace smithy4s.example

use smithy4s.api#simpleRestJson
use smithy4s.api#discriminated

@simpleRestJson
service DiscriminatedService {
  version: "1.0.0",
  operations: [TestDiscriminated]
}


@readonly
@http(method: "GET", uri: "/test/{key}", code: 200)
operation TestDiscriminated {
  input: TestDiscriminatedInput,
  output: TestDiscriminatedOutput,
  errors: []
}

structure TestDiscriminatedInput {
    @required
    @httpLabel
    key: String
}

structure TestDiscriminatedOutput {
  @httpPayload
  data: PayloadData
}

structure PayloadData {
  testBiggerUnion: TestBiggerUnion
}

@discriminated("tpe")
union TestBiggerUnion {
  one: One,
  two: Two
}

structure One {
  value: String
}

structure Two {
  value: Integer
}
