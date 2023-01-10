$version: "2"

namespace smithy4s.example.test

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests
use alloy#simpleRestJson

@simpleRestJson
service HelloService {
    operations: [SayHello, Listen, TestPath,TestPayloadStructure]
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
        params: { payload: { result: "Hello!" }, header1: "V1" }
        body: "{\"result\":\"Hello!\"}"
        headers: { "X-H1": "V1"}
        code: 200
    }
])
operation SayHello {
    input: SayHelloInput,
    output: SayHelloOutput
    errors: [SimpleError, ComplexError]
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
    @httpPayload
    payload: SayHelloPayload

    @required
    @httpHeader("X-H1")
    header1: String
}
structure SayHelloPayload {
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

// The following shapes are used by the documentation
@simpleRestJson
service HelloWorldService {
  version: "1.0.0",
  operations: [Hello]
}
@httpRequestTests([
    {
        id: "helloSuccess"
        protocol: simpleRestJson
        method: "POST"
        uri: "/World"
        params: { name: "World" }
    },
    {
        id: "helloFails"
        protocol: simpleRestJson
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

@httpResponseTests([
    {
        id: "simple_error"
        protocol: simpleRestJson
        params: { expected: -1 }
        code: 400
        body: "{\"expected\":-1}"
        bodyMediaType: "application/json"
        requireHeaders: ["X-Error-Type"]
    }
])
@error("client")
structure SimpleError {
    @required
    expected: Integer
}
@httpResponseTests([
    {
        id: "complex_error"
        protocol: simpleRestJson
        params: { value: -1, message: "some error message", details: { date: 123, location: "NYC"} }
        code: 504
        body: "{\"value\":-1,\"message\":\"some error message\",\"details\":{\"date\":123,\"location\":\"NYC\"}}"
        bodyMediaType: "application/json"
        requireHeaders: ["X-Error-Type"]
    },
    {
        id: "complex_error_no_details"
        protocol: simpleRestJson
        params: { value: -1, message: "some error message" }
        code: 504
        body: "{\"value\":-1,\"message\":\"some error message\"}"
        bodyMediaType: "application/json"
        requireHeaders: ["X-Error-Type"]
    }
])
@error("server")
@httpError(504)
structure ComplexError {
    @required
    value: Integer
    @required
    message: String
    details: ErrorDetails
}

structure ErrorDetails {
    @required
    @timestampFormat("epoch-seconds")
    date: Timestamp
    @required
    location: String
}

@idempotent
@http(uri: "/payload", method: "POST")
operation TestPayloadStructure {
    input: TestPayloadStructureInputOutput,
    output: TestPayloadStructureInputOutput
}
apply TestPayloadStructure @httpRequestTests([
    {
        id: "RestJsonHttpWithHeadersButNoPayload",
        documentation: "Serializes an request with header members but no payload",
        protocol: simpleRestJson,
        method: "POST",
        uri: "/payload",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Amz-Test-Id": "t-12345"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            testId: "t-12345"
        }
    },
    {
        id: "RestJsonHttpWithHeadersButNoPayloadButWithEmptyParens",
        documentation: "Serializes an request with header members but no payload",
        protocol: simpleRestJson,
        method: "POST",
        uri: "/payload",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "Content-Type": "application/json",
            "X-Amz-Test-Id": "t-12345"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            testId: "t-12345"
            payloadConfig:{} // This is the important part
        }
    }
])

structure TestPayloadStructureInputOutput {
    @httpHeader("X-Amz-Test-Id")
    testId: String,

    @httpPayload
    payloadConfig: PayloadConfig
}

structure PayloadConfig {
    data: Integer
}