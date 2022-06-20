$version: "1.0"

metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "smithy4s.api",
        reason: "This is a library namespace."
    }
]


namespace smithy4s.api

@uuidFormat
string UUID

@protocolDefinition(traits: [
    smithy.api#error,
    smithy.api#required,
    smithy.api#pattern,
    smithy.api#range,
    smithy.api#length,
    smithy.api#http,
    smithy.api#httpError,
    smithy.api#httpHeader,
    smithy.api#httpLabel,
    smithy.api#httpPayload,
    smithy.api#httpPrefixHeaders,
    smithy.api#httpQuery,
    smithy.api#httpQueryParams,
    smithy.api#jsonName,
    smithy.api#timestampFormat,
    uncheckedExamples,
    uuidFormat,
    discriminated,
    untagged,
])
@trait(selector: "service")
structure simpleRestJson {
}

@trait(selector: "operation")
@documentation("A version of @examples that is not tied to a validator")
list uncheckedExamples {
    member: UncheckedExample,
}

@private
structure UncheckedExample {
    @required
    title: String,

    documentation: String,

    input: Document,

    output: Document
}


@trait(selector: "string")
structure uuidFormat {
}

@trait(selector: "union :not([trait|smithy4s.api#untagged])")
string discriminated

@trait(selector: "union :not([trait|smithy4s.api#discriminated])")
structure untagged {}
