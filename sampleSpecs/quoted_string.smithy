$version: "2"

namespace smithy4s.example

@documentation("A plain Smithy string")
string AString

structure AStructure {

    astring: AString = "\"Hello World\" with \"quotes\""
}
