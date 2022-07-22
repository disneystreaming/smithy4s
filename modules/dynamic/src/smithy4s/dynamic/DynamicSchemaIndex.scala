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

/**
  * A dynamically loaded schema index, containing existential instances
  * of services and schemas, that can be used to wire protocols together
  * without requiring code generation.
  */
trait DynamicSchemaIndex {
  def allServices: List[DynamicSchemaIndex.ServiceWrapper]
  def getService(shapeId: ShapeId): Option[DynamicSchemaIndex.ServiceWrapper] =
    allServices.find(_.service.id == shapeId)

  def getSchema(shapeId: ShapeId): Option[Schema[_]]
}

object DynamicSchemaIndex extends DynamicSchemaIndexPlatform {

  /**
    * Loads the model from a dynamic representation of smithy models
    * (typically json blobs). This representation is modelled in smithy itself,
    * and code generated by smithy4s.
    *
    * @param model
    */
  def load(
      model: dynamic.model.Model
  ): DynamicSchemaIndex =
    internals.Compiler.compile(model)

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
