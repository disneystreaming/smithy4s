$version: "2"

namespace smithy4s.example

@documentation("This is a simple example of a \"quoted string\"")
string AString

structure AStructure {

    astring: AString = "\"Hello World\" with \"quotes\""
}
