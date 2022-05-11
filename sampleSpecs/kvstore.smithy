namespace smithy4s.example

service KVStore {
  operations: [Get, Set, Delete]
}

operation Set {
  input: KeyValue
}

operation Get {
  input: Key,
  output: Value,
  errors: [NotFoundError]
}

operation Delete {
  input: Key,
  errors: [NotFoundError]
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
structure NotFoundError {
  key: String
}
