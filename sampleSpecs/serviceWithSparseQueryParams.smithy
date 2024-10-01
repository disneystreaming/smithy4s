$version: "2"

namespace smithy4s.example
 

use alloy#simpleRestJson

@simpleRestJson
service ServiceWithSparseQueryParams {
    version: "1.0"
    operations: [GetOperation]
}

@http(method: "GET", uri: "/operation/sparse-query-params")
operation GetOperation {
    input: SparseQueryInput
    
    output: SparseQueryOutput
}

structure SparseQueryInput {
    @required
    @httpQuery("foo")
    foo: SparseStringList
}

structure SparseQueryOutput {
    @required
    foo: SparseStringList
}
