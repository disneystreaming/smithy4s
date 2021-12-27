package smithy4s
package dynamic

case class DynamicEndpoint(
    namespace: String,
    name: String,
    input: Schema[DynData],
    output: Schema[DynData],
    hints: Hints
) extends Endpoint[DynamicOp, DynData, DynData, DynData, Nothing, Nothing] {

  def wrap(
      input: DynData
  ): DynamicOp[DynData, DynData, DynData, Nothing, Nothing] =
    DynamicOp(namespace, name, input)

  def streamedInput: StreamingSchema[Nothing] = StreamingSchema.NoStream

  def streamedOutput: StreamingSchema[Nothing] = StreamingSchema.NoStream

}
