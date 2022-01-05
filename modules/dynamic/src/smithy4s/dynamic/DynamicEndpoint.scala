package smithy4s
package dynamic

case class DynamicEndpoint(
    id: ShapeId,
    input: Schema[DynData],
    output: Schema[DynData],
    hints: Hints
) extends Endpoint[DynamicOp, DynData, DynData, DynData, Nothing, Nothing] {

  def wrap(
      input: DynData
  ): DynamicOp[DynData, DynData, DynData, Nothing, Nothing] =
    DynamicOp(id, input)

  def streamedInput: StreamingSchema[Nothing] = StreamingSchema.NoStream

  def streamedOutput: StreamingSchema[Nothing] = StreamingSchema.NoStream

}
