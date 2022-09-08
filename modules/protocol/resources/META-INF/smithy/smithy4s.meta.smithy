$version: "2.0"

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

// the errorMessage trait marks a structure's field as one that will be used
// for the generated exception's error message.
@trait(
    selector: "structure > member",
    structurallyExclusive: "member"
)
structure errorMessage {}

/// Allows specifying a custom type that smithy4s will use for rendering
/// the model. `targetType` should point to the type that you want
/// to use in the place of the standard smithy4s type. `providerImport`
/// should be an import that will bring in the implicit RefinementProvider
/// for the type specified by `targetType`. `providerImport` is optional
/// because it is unnecessary if the RefinementProvider is inside of the
/// companion object of the `targetType`.
/// For example:
/// namespace test
/// @trait(selector: "string")
/// structure emailFormat {}
///
/// @emailFormat()
/// string Email
/// ---
/// namespace test.meta
/// apply test#emailFormat @refinement(
///   targetType: "myapp.types.Email",
///   providerImport: "myapp.types.Email.provider._"
/// )
///
/// Here we are applying the refinement trait to the `test#emailFormat` trait.
/// We tell it which type it should be represented by in scala code
/// and where to find the provider implicit.
@trait(selector: "* [trait|trait]")
structure refinement {
    @required
    targetType: Classpath,
    providerImport: Import
}

/// e.g. com.test_out.v2.Something
@pattern("^(?:_root_\\.)?(?:[a-zA-Z][\\w]*\\.?)*$")
string Classpath

/// e.g. com.test_out.v2.Something._
@pattern("^(?:_root_\\.)?(?:[a-zA-Z][\\w]*\\.?)*\\._$")
string Import

/// This trait is used to signal that this type should not be wrapped
/// in a newtype at usage sites. For example:
/// 
/// @unwrap
/// string Email
///
/// structure Test {
///   email: Email
/// }
///
/// Here the generated code for the field `email` in the structure `Test`
/// will refer directly to `String` rather than the newtype `Email`.
/// Note that collections (lists, maps, and sets) are already unwrapped at usage sites
/// by default except when the collection has been refined. In this case, it is wrapped
/// by default. Adding this trait will cause the collection to become unwrapped.
@trait(selector: ":is(simpleType, list, map, set)")
structure unwrap {}
