$version: "2"

namespace my.input

@trait(selector: "service")
string apiVersion

@apiVersion("v1")
service FooService {}
