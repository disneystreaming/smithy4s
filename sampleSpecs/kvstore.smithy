namespace smithy4s.example

service KVStore {
  operations: [Get, Put, Delete],
  errors: [UnauthorizedError]
}

operation Put {
  input: KeyValue
}

operation Get {
  input: Key,
  output: Value,
  errors: [KeyNotFoundError]
}

operation Delete {
  input: Key,
  errors: [KeyNotFoundError]
}

structure Key {
  @required
  key: String
}

structure KeyValue {
  @required
  key: String,
  @required
  value: String
}

structure Value {
  @required
  value: String
}

@error("client")
structure UnauthorizedError {
  @required
  reason: String
}

@error("client")
structure KeyNotFoundError {
  @required
  message: String
}
