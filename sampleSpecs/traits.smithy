$version: "2.0"

namespace smithy4s.example.traits

/// A trait with no recursive references to itself
@trait
structure nonRecursiveTrait {}

/// A trait with a direct recursive reference
@trait
@directRecursiveTrait
structure directRecursiveTrait {}

/// A trait with an indirect recursive reference
@trait
@indirectRecursiveTrait1
structure indirectRecursiveTrait0 {}

// trait that completes the recursion of the above, also recursive because it has to loop back through the above
@trait
@indirectRecursiveTrait0
structure indirectRecursiveTrait1 {}

/// A trait with no recursion in itself, referencing recursive traits
@trait
@nonRecursiveTrait
@directRecursiveTrait
@indirectRecursiveTrait0
@indirectRecursiveTrait1
structure nonRecursiveTraitReferencingOthers {}
