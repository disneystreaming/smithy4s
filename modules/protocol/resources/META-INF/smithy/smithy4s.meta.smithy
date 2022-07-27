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

/// the adtMember trait can be added to structures that are targeted by
/// a single union. This trait tells smithy4s to generate the code
/// such that the structure directly extends the union's sealed trait.
/// This makes it so the structure can be used directly was a member of
/// the union rather than being wrapped in a `MyStructureCase` class
/// which is the default behavior.
/// Example usage: @adtMember(MyUnion)
@trait(selector: "structure :not([trait|error])")
@idRef(failWhenMissing: true, selector: "union")
string adtMember

// the indexedSeq trait can be added to list shapes in order for the generated collection
// fields to be of type `IndexedSeq` instead of `List`. When decoding instances of IndexedSeq
// from various formats, Smithy4s will do a best effort to try and back the IndexedSeq
// the most efficiently possible, often using `ArraySeq` and storing primitive values
// in unboxed ways.
@trait(selector: """
        list
        :not(:test([trait|smithy4s.meta#vector],
                   [trait|smithy.api#uniqueItems]))""")
structure indexedSeq {}

// the vector trait can be added to list shapes in order for the generated collection
// fields to be of type `Vector` instead of `List`
@trait(selector: """
        list
        :not(:test([trait|smithy4s.meta#indexedSeq],
                   [trait|smithy.api#uniqueItems]))""")
structure vector {}

/// Allows specifying a custom type that smithy4s will use for rendering
/// the model. `targetClasspath` should point to the type that you want
/// to use in the place of the standard smithy4s type. `providerClasspath`
/// should point to an instance of the RefinementProvider for the type specified by `targetClasspath`.
/// For example:
/// namespace test
/// @trait(selector: "string")
/// structure emailFormat {}
///
/// @emailFormat()
/// string Email
/// ---
/// namespace test.meta
/// apply test#emailFormat @refined(
///   targetClasspath: "myapp.types.Email",
///   providerClasspath: "myapp.types.Email.provider"
/// )
///
/// Here we are applying the refined trait to the `test#emailFormat` trait.
/// We tell it which type it should be represented by in scala code
/// and where to find the provider.
@trait(selector: "* [trait|trait]")
structure refined {
    @required
    targetClasspath: Classpath,
    @required
    providerClasspath: Classpath
}

@pattern("^(?:[a-zA-Z][\\w]*\\.?)*$")
string Classpath
