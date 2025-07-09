$version: "2.0"

metadata suppressions = [
    {
        id: "UnreferencedShape"
        namespace: "smithy4s.meta"
        reason: "This is a library namespace."
    }
]

namespace smithy4s.meta

@tags(["nocodegen"])
@trait(selector: ":is(operation)")
structure only {}

@tags(["nocodegen"])
@trait(selector: ":is(service, operation)")
structure packedInputs {}

/// the adtMember trait can be added to structures that are targeted by
/// a single union. This trait tells smithy4s to generate the code
/// such that the structure directly extends the union's sealed trait.
/// This makes it so the structure can be used directly was a member of
/// the union rather than being wrapped in a `MyStructureCase` class
/// which is the default behavior.
/// Example usage: @adtMember(MyUnion)
@tags(["nocodegen"])
@trait(selector: "structure :not([trait|error])")
@idRef(failWhenMissing: true, selector: "union :not([trait|mixin])")
string adtMember

/// Implies that all members of the union are annotated with the `adtMember` trait.
/// Further signals that the `sealed trait` for this adt will extend the traits
/// defined by any mixins that are present on all of the adt members.
@tags(["nocodegen"])
@trait(selector: "union :not([trait|mixin])")
structure adt {}

// the indexedSeq trait can be added to list shapes in order for the generated collection
// fields to be of type `IndexedSeq` instead of `List`. When decoding instances of IndexedSeq
// from various formats, Smithy4s will do a best effort to try and back the IndexedSeq
// the most efficiently possible, often using `ArraySeq` and storing primitive values
// in unboxed ways.
@tags(["nocodegen"])
@trait(
    selector: """
        list
        :not(:test([trait|smithy4s.meta#vector],
                   [trait|smithy.api#uniqueItems]))"""
)
structure indexedSeq {}

// the vector trait can be added to list shapes in order for the generated collection
// fields to be of type `Vector` instead of `List`
@tags(["nocodegen"])
@trait(
    selector: """
        list
        :not(:test([trait|smithy4s.meta#indexedSeq],
                   [trait|smithy.api#uniqueItems]))"""
)
structure vector {}

// the errorMessage trait marks a structure's field as one that will be used
// for the generated exception's error message.
@tags(["nocodegen"])
@trait(selector: "structure > member", structurallyExclusive: "member")
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
@tags(["nocodegen"])
@trait(selector: "* [trait|trait]")
structure refinement {
    @required
    targetType: Classpath

    providerImport: Import

    parameterised: Boolean = false
}

/// e.g. com.test_out.v2.Something
/// e.g. com.test_out.v2.`Something`
@pattern("^(?:_root_\\.)?(?:[a-zA-Z`][\\w]*\\.?)*$")
string Classpath

/// e.g. com.test_out.v2.Something._
/// e.g. com.test_out.v2.`Something`._
@pattern("^(?:_root_\\.)?(?:[a-zA-Z`][\\w]*\\.?)*\\.(?:_|given)$")
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
@tags(["nocodegen"])
@trait(selector: ":is(simpleType, list, map, set)")
structure unwrap {}

/// Placing this trait on another trait marks the target trait as a
/// typeclass. This means that shapes which are marked with the target
/// trait will have an instance of the typeclass made available in the
/// generated object companion.
///
/// For example,
///
/// @typeclass(targetType: "cats.Show", interpreter: "my.show.Interpreter")
/// @trait
/// structure show {}
///
/// @show
/// structure Person {
///   name: String
/// }
///
/// This example would lead to generated code where the Person
/// case class has a `cats.Show` instance available in its companion
/// object.
@tags(["nocodegen"])
@trait(selector: "* [trait|trait]")
structure typeclass {
    @required
    targetType: Classpath

    @required
    interpreter: Classpath
}

/// Placing this trait on a service will cause the generated code to
/// include a Service Product version of the service.
@tags(["nocodegen"])
@trait(selector: ":is(service)")
structure generateServiceProduct {}

/// Placing this trait on a shape will cause the generated
/// code to have optics (Lenses or Prisms) in the companion
/// object.
@tags(["nocodegen"])
@trait(selector: ":is(enum, intEnum, union, structure)")
structure generateOptics {}

/// Placing this trait on an error will cause the generated code to exclude the stacktrace
///  via extending scala.util.control.NoStackTrace instead of Throwable.
@tags(["nocodegen"])
@trait(selector: "structure :is([trait|error])")
structure noStackTrace {}

/// Allows users to manually add imports to files of generated shapes.
/// This would be helpful when some shape needs specific import(s) in order
/// to compile. Especially in the case you want to compose refinement types
/// and other validators.
@tags(["nocodegen"])
@trait
list scalaImports {
    member: Import
}

@tags(["nocodegen"])
@trait(
    selector: """
        :is(
            number[trait|range],
            string[trait|pattern],
            string[trait|length]
        )"""
    conflicts: [unwrap]
)
structure validateNewtype {}

/// Marks the given shape to be generated in a way that allows binary-compatible evolution.
/// For example, classes generated from such structures will not have a public copy method, but will have .withXXX methods instead.
/// Unions will be generated without direct access to member classes, and their visitor will require a default value.
@trait(
    selector: ":test(structure, union, enum, intEnum)"
    conflicts: [adt, adtMember]
)
@traitValidators({
    "bincompatFriendly.NoErrors": { selector: "[trait|error]", message: "A @bincompatFriendly structure must not have the error trait." }
    "bincompatFriendly.NoInputOutput": { selector: ":in(:root(operation :is(-[input]->, -[output]->)))", message: "A @bincompatFriendly structure must not be used as an operation input/output." }
    "bincompatFriendly.NoAdtMemberTargets": { selector: "union > member:test(> [trait|smithy4s.meta#adtMember])", message: "Members of an @bincompatFriendly union must not target shapes that have the adtMember trait." }
    "bincompatFriendly.NoAdtTargets": { selector: "< :in(:root(union[trait|smithy4s.meta#adt] > member))", message: "Shapes with the @bincompatFriendly trait must not be used as members of an adt union." }
})
structure bincompatFriendly {}

/// Marks the given member shape as one that was added to the structure _after_ it was initially created and its generated code was published.
/// Adding such members is a change that keeps binary compatibility.
@trait(selector: "structure[trait|smithy4s.meta#bincompatFriendly] > member")
@traitValidators({
    "bincompatAdded.MustHaveDefault": { selector: "[trait|required]:not([trait|default])", message: "A @bincompatAdded required member must have a default value." }
})
structure bincompatAdded {
    /// Used to determine which members of the structure were added, and in what order.
    /// Members marked with the same version will be grouped together, and appended to the previous version's members in any generated constructors.
    /// You must not add new members with a version number that's already been published.
    /// The version must consist of a sequence of dot-separated numbers, e.g. "1.0", "1.2.3", "2.0.0", "1.2.3.4".
    @required
    @pattern("^(\\d+\\.)*\\d+$")
    version: String
}
