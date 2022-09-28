$version: "2"

namespace smithy4s.example

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use smithy4s.api#simpleRestJson

@simpleRestJson
service HelloService {
    operations: [SayHello, Listen, TestPath]
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
@httpResponseTests([
    {
        id: "say_hello"
        protocol: simpleRestJson
        params: { result: "Hello!" }
        body: "{\"result\":\"Hello!\"}"
        code: 200
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


@http(method: "GET", uri: "/listen")
@readonly
@httpRequestTests([
    {
        id: "listen",
        protocol: simpleRestJson,
        method: "GET",
        uri: "/listen"
    }
])
operation Listen { }

@http(method: "GET", uri: "/test-path/{path}")
@readonly
@httpRequestTests([
    {
        id: "TestPath",
        protocol: simpleRestJson,
        method: "GET",
        uri: "/test-path/sameValue"
        params: { path: "sameValue" }
    }
])
operation TestPath {
    input := {
        @httpLabel
        @required
        path: String
    }
}