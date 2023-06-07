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
    endpoints: List[DynamicEndpoint],
    hints: Hints
) extends Service.Reflective[DynamicOp]
    with DynamicSchemaIndex.ServiceWrapper {

  private lazy val endpointMap: Map[ShapeId, Endpoint[_, _, _, _, _]] =
    endpoints.map(ep => ep.id -> ep).toMap

  def getEndpoint[I, E, O, SI, SO](
      id: ShapeId
  ): Endpoint[I, E, O, SI, SO] =
    endpointMap
      .getOrElse(id, sys.error("Unknown endpoint: " + id))
      .asInstanceOf[Endpoint[I, E, O, SI, SO]]

  def endpoint[I, E, O, SI, SO](
      op: DynamicOp[I, E, O, SI, SO]
  ): (I, Endpoint[I, E, O, SI, SO]) = {
    val endpoint = getEndpoint[I, E, O, SI, SO](op.id)
    val input = op.data
    (input, endpoint)
  }

  type Alg[P[_, _, _, _, _]] = PolyFunction5[DynamicOp, P]

  override val service: Service[Alg] = this

  type StaticAlg[P[_, _, _, _, _]] = PolyFunction5[NoInputOp, P]

  val static
      : StaticService.Aux[StaticAlg, PolyFunction5.From[DynamicOp]#Algebra] =
    new StDynamicService(
      this
    )

}

// TODO: better name? different notions of static vs dynamic here
private[internals] class StDynamicService(
    override val service: DynamicService
) extends StaticService[PolyFunction5.From[NoInputOp]#Algebra] {

  type Alg[F[_, _, _, _, _]] = PolyFunction5[DynamicOp, F]

  override def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](
      alg: PolyFunction5[NoInputOp, F],
      function: PolyFunction5[F, G]
  ): PolyFunction5[NoInputOp, G] =
    alg.andThen(function)

  override val endpoints: PolyFunction5[NoInputOp, service.Endpoint] =
    new PolyFunction5[NoInputOp, service.Endpoint] {
      override def apply[I, E, O, SI, SO](
          op: NoInputOp[I, E, O, SI, SO]
      ): Endpoint[DynamicOp, I, E, O, SI, SO] =
        service.getEndpoint(op.id)
    }

  override def toPolyFunction[P2[_, _, _, _, _]](
      algebra: PolyFunction5[NoInputOp, P2]
  ): PolyFunction5[service.Endpoint, P2] =
    new PolyFunction5[service.Endpoint, P2] {
      override def apply[I, E, O, SI, SO](
          endpoint: smithy4s.Endpoint[DynamicOp, I, E, O, SI, SO]
      ): P2[I, E, O, SI, SO] = {
        algebra.apply(NoInputOp(endpoint.id))
      }
    }

}
