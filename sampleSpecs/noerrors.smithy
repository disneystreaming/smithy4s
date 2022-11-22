$version: "2.0"

namespace smithy4s.example

@smithy4s.api#simpleRestJson
service EchoService {
  version: "1.0.0",
  // this service must NOT have any errors!
  errors: [],
  operations: [Echo]
}

@http(method: "POST", uri: "/echo/{pathParam}")
operation Echo {
  input := {
    @required
    @httpLabel
    @length(min: 5)
    pathParam: String,

    @httpQuery("queryParam")
    @length(min: 5)
    queryParam: String,

    @httpPayload
    @length(min: 5)
    body: String
  }
  // this operation must NOT have any errors
  errors: []
}
