$version: "2"

namespace smithy4s.example.greet

service GreetService {
  operations: [Greet]
}

operation Greet {
  input := {
    @required
    name: String
  }
  output := {
    @required
    message: String
  }
}
