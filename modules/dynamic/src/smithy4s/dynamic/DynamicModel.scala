/*
 *  Copyright 2021 Disney Streaming
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

class DynamicModel(
    serviceMap: Map[ShapeId, DynamicService],
    schemaMap: Map[ShapeId, Schema[DynData]]
) {

  def allServices: List[DynamicModel.ServiceWrapper] =
    serviceMap.values.toList

  def getSchema(
      namespace: String,
      name: String
  ): Option[Schema[_]] = {
    val shapeId = ShapeId(namespace, name)
    schemaMap.get(shapeId)
  }
}

object DynamicModel {

  /**
    * A construct that hides the types a service instance works,
    * virtually turning them into existential types.
    *
    * This prevents the user from calling the algebra/transformation
    * in an unsafe fashion.
    */
  trait ServiceWrapper {
    type Alg[P[_, _, _, _, _]]
    type Op[I, E, O, SI, SO]

    def service: Service[Alg, Op]
  }

}
