$version: "2"

namespace smithy4s.example

@documentation("This is a simple example of a \"quoted string\"")
string AString

/// Multiple line doc comment for another string
/// Containing a random */ here.
/// Seriously, it's important to escape special characters.
string AnotherString

structure AStructure {
    astring: AString = "\"Hello World\" with \"quotes\""
}

@documentation("This is meant to be used with ${ENV_VAR}")
structure EnvVarString {
    @documentation("This is meant to be used with $ENV_VAR")
    member: String
}
