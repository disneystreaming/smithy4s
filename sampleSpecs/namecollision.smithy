$version: "2.0"

namespace smithy4s.example


use smithy4s.api#simpleRestJson

@simpleRestJson
service MonadiWorldService {
    version: "1.0.0",
    // Indicates that all operations in `HelloWorldService`,
    // here limited to Hello, can return `GenericServerError`.
    errors: [GenericServerError],
    operations: [Hello]
}

@error("server")
@httpError(500)
structure Monadic {
    message: String
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting
}


structure Person {
    @httpLabel
    @required
    name: String,

    @httpQuery("town")
    town: String
}

structure Greeting {
    @required
    message: String
}

