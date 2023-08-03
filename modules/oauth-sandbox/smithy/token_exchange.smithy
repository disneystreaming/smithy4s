$version: "2"

namespace smithy4s.sandbox.oauth

// TODO: Move out of sandbox.

@protocolDefinition
@trait(selector: "service")
structure tokenExchange {}

// TODO: Make UrlFormSchemaVisitors work with this.

/// Unwraps the values of a list, set, or map into the containing /
//structure/union.
@trait(
    selector: ":is(structure, union) > :test(member > :test(list, map))",
    breakingChanges: [{change: "any"}]
)
structure queryFlattened {}

// TODO: Make UrlFormSchemaVisitors work with this.

/// The queryName trait allows a serialized form key to differ from a structure
/// member name used in the model.
@trait(
    selector: ":is(structure, union) > member",
    breakingChanges: [{change: "any"}]
)
string queryName
