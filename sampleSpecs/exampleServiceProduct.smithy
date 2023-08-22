$version: "2"

namespace smithy4s.example.product

use smithy4s.meta#generateServiceProduct

@generateServiceProduct
service ExampleService {
    operations: [ExampleOperation]
}

operation ExampleOperation {
    input := {
        @required
        a: String
    }
    output := {
        @required
        b: String
    }
}
