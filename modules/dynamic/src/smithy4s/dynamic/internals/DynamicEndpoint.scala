/*
 *  Copyright 2021-2022 Disney Streaming
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
package dynamic
package internals

private[internals] case class DynamicEndpoint(
    id: ShapeId,
    input: Schema[DynData],
    output: Schema[DynData],
    override val errorable: Option[Errorable[DynData]],
    hints: Hints
) extends Endpoint[DynamicOp, DynData, DynData, DynData, Nothing, Nothing] {

  def wrap(
      input: DynData
  ): DynamicOp[DynData, DynData, DynData, Nothing, Nothing] =
    DynamicOp(id, input)

  def streamedInput: StreamingSchema[Nothing] = StreamingSchema.NoStream

  def streamedOutput: StreamingSchema[Nothing] = StreamingSchema.NoStream

}
