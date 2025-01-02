$version: "2.0"

namespace smithy4s.hello

service AdminService {
  operations: [GetUser]
}

@http(method: "GET", uri: "/user/{id}")
operation GetUser {
  input := {
    @required
    @httpLabel
    id: String
  }
  output: User
}

structure User {
  @required firstName: String
  @required lastName: String
}
