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

package smithy4s.dynamic

import smithy4s.Service
import smithy4s.schema.Schema
import smithy4s.ShapeId
import smithy4s.Document

/**
  * Builder for creating a `DynamicSchemaIndex` manually
  */
sealed case class DynamicSchemaIndexBuilder private (
    services: List[DynamicSchemaIndex.ServiceWrapper],
    schemas: Map[ShapeId, Schema[_]]
) {

  import DynamicSchemaIndexBuilder._

  /**
    * Add a `smithy4s.Service` which will be included when building the `DynamicSchemaIndex`
    */
  def addService[Alg0[_[_, _, _, _, _]]](
      service: Service[Alg0]
  ): DynamicSchemaIndexBuilder =
    this.copy(services = this.services :+ WrappedService(service))

  /**
    * Add one or more `smithy4s.Schema`s which will be included when building the `DynamicSchemaIndex`
    */
  def addSchemas(schemas: Schema[_]*): DynamicSchemaIndexBuilder =
    this.copy(schemas =
      this.schemas ++ schemas.map(schema => (schema.shapeId -> schema)).toMap
    )

  /**
    * Build the DynamicSchemaIndex using the information in this builder.
    * 
    * Note that the functions in this `DynamicSchemaIndex`, including `getServices`,
    * `allSchemas`, and `getSchema` will only return the items that were placed into
    * the builder directly and NOT the transitive values. Additionally, the metadata
    * is always empty.
    */
  def build(): DynamicSchemaIndex =
    new DynamicSchemaIndex {
      def allServices: Iterable[DynamicSchemaIndex.ServiceWrapper] = services

      def allSchemas: Iterable[Schema[_]] = schemas.values

      def getSchema(shapeId: ShapeId): Option[Schema[_]] = schemas.get(shapeId)

      def metadata: Map[String, Document] = Map.empty

    }

}

object DynamicSchemaIndexBuilder {
  def apply(): DynamicSchemaIndexBuilder =
    new DynamicSchemaIndexBuilder(List.empty, Map.empty)

  private final case class WrappedService[Alg0[_[_, _, _, _, _]]](
      val svc: Service[Alg0]
  ) extends DynamicSchemaIndex.ServiceWrapper {
    type Alg[P[_, _, _, _, _]] = Alg0[P]
    def service: Service[Alg] = svc
  }
}
