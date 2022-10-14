$version: "2"

namespace smithy4s.example

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use alloy#restJson

@restJson
service HelloService {
    operations: [SayHello, Listen, TestPath]
}

@http(method: "POST", uri: "/")
@httpRequestTests([
    {
        id: "say_hello",
        protocol: restJson,
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
        protocol: restJson
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
        protocol: restJson,
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
        protocol: restJson,
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

// The following shapes are used
@restJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
}
@httpRequestTests([
    {
        id: "helloSuccess"
        protocol: restJson
        method: "POST"
        uri: "/World"
        params: { name: "World" }
    },
    {
        id: "helloFails"
        protocol: restJson
        method: "POST"
        uri: "/fail"
        params: { name: "World" }
    }
])
@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
  input := {
    @httpLabel
    @required
    name: String
  },
  output := {
    @required
    message: String
  }
}
