namespace smithy4s.example.import_test

@http(method: "GET", uri: "/test", code: 200)
operation ImportOperation {
  output: OpOutput
}

structure OpOutput {
  @httpPayload
  @required
  output: String
}
