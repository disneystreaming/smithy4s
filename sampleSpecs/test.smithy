namespace smithy4s.example

use smithy.test#httpRequestTests
use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloService {
    operations: [SayHello]
}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "say_hello",
        protocol: simpleRestJson,
        params: {
            "greeting": "Hi",
            "name": "Teddy",
            "query": "Hello there"
        },
        method: "POST",
        uri: "/",
        queryParams: [
            "Hi=Hello%20there"
        ],
        headers: {
            "X-Greeting": "Hi",
        },
        body: "{\"name\":\"Teddy\"}",
        bodyMediaType: "application/json"
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
}

@input
structure SayHelloInput {
    @httpHeader("X-Greeting")
    greeting: String,

    @httpQuery("Hi")
    query: String,

    name: String
}

structure SayHelloOutput {
    @required
    result: String
}
