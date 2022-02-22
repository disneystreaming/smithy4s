namespace smithy4s.example

use smithy4s.meta#packedInputs

@packedInputs
service PackedInputsService {
  version: "1.0.0",
  operations: [PackedInputOperation]
}

operation PackedInputOperation {
  input: PackedInput,
}

structure PackedInput {
    @required
    key: String
}
