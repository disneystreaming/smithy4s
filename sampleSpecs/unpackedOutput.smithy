$version: "2.0"

namespace smithy4s.example

use smithy4s.meta#unpackedOutput

service UnpackedOutputService {
    version: "1.0.0"
    operations: [
        GetRequiredItem
        GetOptionalItem
    ]
}

@unpackedOutput
operation GetRequiredItem {
    input := {}
    output := {
        @required
        item: UnpackedItem
    }
}

@unpackedOutput
operation GetOptionalItem {
    input := {}
    output := {
        item: UnpackedItem
    }
}

structure UnpackedItem {
    @required
    id: String
}
