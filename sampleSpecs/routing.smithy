$version: "2"

namespace smithy4s.routing

use alloy#simpleRestJson

@simpleRestJson
service RoutingService {
  version: "1.0.0"
  operations: [
    Abc
    AbcDef
    AbcLabel
    GreedyAbcDef
  ]
}

@readonly
@http(method: "GET", uri: "/abc", code: 200)
operation Abc {
  output: MessageOutput
}


@readonly
@http(method: "GET", uri: "/abc/def", code: 200)
operation AbcDef {
  output: MessageOutput
}

@readonly
@http(method: "GET", uri: "/abc/{def}", code: 200)
operation AbcLabel {
  input: AbcLabelInput
  output: MessageOutput
}

structure AbcLabelInput {
  @httpLabel
  @required
  def: String
}

@readonly
@http(method: "GET", uri: "/abc/{def+}", code: 200)
operation GreedyAbcDef {
  input: GreedyAbcDefInput
  output: MessageOutput
}

structure GreedyAbcDefInput {
  @httpLabel
  @required
  def: String
}

structure MessageOutput {
  @httpPayload
  @required
  message: String
}
