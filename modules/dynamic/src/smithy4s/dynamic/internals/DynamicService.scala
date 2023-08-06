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

import smithy4s.kinds.PolyFunction5

private[internals] case class DynamicService(
    id: ShapeId,
    version: String,
    endpoints: Vector[DynamicEndpoint],
    hints: Hints
) extends Service.Reflective[DynamicOp]
    with DynamicSchemaIndex.ServiceWrapper {

  type Alg[P[_, _, _, _, _]] = PolyFunction5.From[DynamicOp]#Algebra[P]
  override val service: Service[Alg] = this

  private lazy val ordinalMap: Map[ShapeId, Int] =
    endpoints.map(_.id).zipWithIndex.toMap

  def input[I, E, O, SI, SO](op: DynamicOp[I, E, O, SI, SO]): I = op.data
  def ordinal[I, E, O, SI, SO](op: DynamicOp[I, E, O, SI, SO]): Int =
    ordinalMap(op.id)

}
