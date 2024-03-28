namespace smithy4s.example

use alloy#simpleRestJson
use alloy#UUID

service StreamedObjects {
  version: "1.0.0",
  operations: [PutStreamedObject, GetStreamedObject, PutAndGetStreamedObject]
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
    @documentation("data docs")
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

operation PutAndGetStreamedObject {
  input: PutStreamedObjectInput,
  output: GetStreamedObjectOutput
}

@streaming
blob StreamedBlob


