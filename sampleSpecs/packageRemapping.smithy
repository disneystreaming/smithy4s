$version: "2"

metadata smithy4sCodegen = {
    packageMappings: {
        "pkg.remapping": "smithy4s.example.packageremapping"
        "pkg.remapping.nested": "smithy4s.example.packageremapping.nested"
    }
}

namespace pkg.remapping

use pkg.remapping.nested#NestedString

structure PackageRemappingExample {
    @required
    value: NestedString
}
