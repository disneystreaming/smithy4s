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

@protocolDefinition
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

@trait(selector: "union")
structure discriminated {
    propertyName: String
}
