namespace smithy4s.example

use smithy4s.api#simpleRestJson

@simpleRestJson
service RecursiveInputService {
  version: "0.0.1",
  operations: [RecursiveInputOperation],
}

@http(method: "PUT", uri: "/subscriptions")
@idempotent
operation RecursiveInputOperation {
  input: RecursiveInput,
}

structure RecursiveInput {
  hello: RecursiveInput
}
