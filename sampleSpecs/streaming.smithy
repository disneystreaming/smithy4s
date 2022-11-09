namespace smithy4s.example

use alloy#simpleRestJson
use alloy#UUID

service StreamedObjects {
  version: "1.0.0",
  operations: [PutStreamedObject, GetStreamedObject]
}

operation PutStreamedObject {
  input: PutStreamedObjectInput,
}

operation GetStreamedObject {
  input: GetStreamedObjectInput,
  output: GetStreamedObjectOutput
}

structure PutStreamedObjectInput {
    @required
    key: String,
    data: StreamedBlob
}

structure GetStreamedObjectInput {
    // Sent in the URI label named "key".
    @required
    key: String
}

structure GetStreamedObjectOutput {
  data: StreamedBlob
}

@streaming
blob StreamedBlob


