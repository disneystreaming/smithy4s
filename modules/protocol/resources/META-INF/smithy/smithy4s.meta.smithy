$version: "1.0"

metadata suppressions = [
    {
        id: "UnreferencedShape",
        namespace: "smithy4s.meta",
        reason: "This is a library namespace."
    }
]


namespace smithy4s.meta

@trait(selector: ":is(service, operation)")
structure packedInputs {}

@trait(selector: "structure :not([trait|error])")
@idRef(failWhenMissing: true, selector: "union")
string adtMember
