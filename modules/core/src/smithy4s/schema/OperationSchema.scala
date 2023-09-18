package smithy4s

package schema

import smithy4s.Errorable

final case class OperationSchema[I, E, O, SI, SO](
    id: ShapeId,
    hints: Hints,
    input: Schema[I],
    error: Option[Errorable[E]],
    output: Schema[O],
    streamedInput: Option[StreamingSchema[SI]],
    streamedOutput: Option[StreamingSchema[SO]]
) {

  def withId(id: ShapeId): OperationSchema[I, E, O, SI, SO] = copy(id = id)

  def mapId(f: ShapeId => ShapeId): OperationSchema[I, E, O, SI, SO] =
    copy(id = f(id))

  def withHints(hints: Hints): OperationSchema[I, E, O, SI, SO] =
    copy(hints = hints)

  def withHints(hints: Hint*): OperationSchema[I, E, O, SI, SO] =
    copy(hints = Hints.fromSeq(hints))

  def mapHints(f: Hints => Hints): OperationSchema[I, E, O, SI, SO] =
    copy(hints = f(hints))

  def withInput[I2](input: Schema[I2]): OperationSchema[I2, E, O, SI, SO] =
    copy(input = input)

  def mapInput[I2](
      f: Schema[I] => Schema[I2]
  ): OperationSchema[I2, E, O, SI, SO] =
    copy(input = f(input))

  def withOutput[O2](output: Schema[O2]): OperationSchema[I, E, O2, SI, SO] =
    copy(output = output)

  def mapOutput[O2](
      f: Schema[O] => Schema[O2]
  ): OperationSchema[I, E, O2, SI, SO] =
    copy(output = f(output))

  def withError[E2](
      error: Errorable[E2]
  ): OperationSchema[I, E2, O, SI, SO] = copy(error = Some(error))

  def withoutError[E2 <: E]: OperationSchema[I, E2, O, SI, SO] =
    copy(error = None)

  def withErrorOption[E2](
      error: Option[Errorable[E2]]
  ): OperationSchema[I, E2, O, SI, SO] = copy(error = error)

  def mapError[E2](
      f: Errorable[E] => Errorable[E2]
  ): OperationSchema[I, E2, O, SI, SO] = copy(error = error.map(f))

  def withStreamedInput[SI2](
      streamedInput: StreamingSchema[SI2]
  ): OperationSchema[I, E, O, SI2, SO] =
    copy(streamedInput = Some(streamedInput))

  def mapStreamedInput[SI2](
      f: StreamingSchema[SI] => StreamingSchema[SI2]
  ): OperationSchema[I, E, O, SI2, SO] =
    copy(streamedInput = streamedInput.map(f))

  def withStreamedOutput[SO2](
      streamedOutput: StreamingSchema[SO2]
  ): OperationSchema[I, E, O, SI, SO2] =
    copy(streamedOutput = Some(streamedOutput))

  def mapStreamedOutput[SO2](
      f: StreamingSchema[SO] => StreamingSchema[SO2]
  ): OperationSchema[I, E, O, SI, SO2] =
    copy(streamedOutput = streamedOutput.map(f))

}
