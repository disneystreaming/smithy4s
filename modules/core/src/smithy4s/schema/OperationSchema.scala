/*
 *  Copyright 2021-2024 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s

package schema

import smithy4s.internals.InputOutput

final case class OperationSchema[I, E, O, SI, SO] private[smithy4s] (
    id: ShapeId,
    hints: Hints,
    input: Schema[I],
    error: Option[ErrorSchema[E]],
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
    copy(input = input.addHints(InputOutput.Input.widen))

  def mapInput[I2](
      f: Schema[I] => Schema[I2]
  ): OperationSchema[I2, E, O, SI, SO] =
    withInput(f(input))

  def withOutput[O2](output: Schema[O2]): OperationSchema[I, E, O2, SI, SO] =
    copy(output = output.addHints(InputOutput.Output.widen))

  def mapOutput[O2](
      f: Schema[O] => Schema[O2]
  ): OperationSchema[I, E, O2, SI, SO] =
    withOutput(f(output))

  def withError[E2](
      error: ErrorSchema[E2]
  ): OperationSchema[I, E2, O, SI, SO] = copy(error = Some(error))

  def withoutError[E2 <: E]: OperationSchema[I, E2, O, SI, SO] =
    copy(error = None)

  def withErrorOption[E2](
      error: Option[ErrorSchema[E2]]
  ): OperationSchema[I, E2, O, SI, SO] = copy(error = error)

  def mapError[E2](
      f: ErrorSchema[E] => ErrorSchema[E2]
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

object OperationSchema {
  def apply[I, E, O, SI, SO](
      id: ShapeId,
      hints: Hints,
      input: Schema[I],
      error: Option[ErrorSchema[E]],
      output: Schema[O],
      streamedInput: Option[StreamingSchema[SI]],
      streamedOutput: Option[StreamingSchema[SO]]
  ): OperationSchema[I, E, O, SI, SO] = {
    new OperationSchema(
      id,
      hints,
      input,
      error,
      output,
      streamedInput,
      streamedOutput
    )
  }

}
