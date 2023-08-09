$version: "2"

namespace alloy

// TODO: Move out of smithy4s.
@protocolDefinition
@trait(selector: "service")
structure tokenExchange {}

// TODO: Move to alloy and make UrlFormSchemaEncoder/DecoderVisitors work with this.
/// Unwraps the values of a list, set, or map into the containing
/// structure/union.
@trait(
    selector: ":is(structure, union) > :test(member > :test(list, map))",
    breakingChanges: [{change: "any"}]
)
structure queryFlattened {}

// TODO: Move to alloy and make UrlFormSchemaEncoder/DecoderVisitors work with this.
/// Changes the serialized key of a structure, union, or member.
@trait(
    selector: ":is(structure, union) > member",
    breakingChanges: [{change: "any"}]
)
string queryName
