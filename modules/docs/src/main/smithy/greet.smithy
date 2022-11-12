$version: "2"

namespace foo

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
