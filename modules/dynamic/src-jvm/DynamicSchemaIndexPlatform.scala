/*
 *  Copyright 2021-2025 Disney Streaming
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
import smithy4s.schema.OperationSchema
import software.amazon.smithy.model.shapes.Shape
import smithy4s.schema.Schema
import software.amazon.smithy.model.shapes.OperationShape
import scala.jdk.CollectionConverters._
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.Model

private[dynamic] trait DynamicSchemaIndexPlatform {
  self: DynamicSchemaIndex =>

  import smithy4s.dynamic.internals.conversion.syntax._

  /**
    * Convert the `DynamicSchemaIndex` into a smithy `Model`.
    */
  def toSmithyModel: Model = {
    val allShapes =
      self.allSchemas.flatMap(fromSchema(_)) ++
        self.allServices.flatMap(s => fromService(s.service))
    Model.builder().addShapes(allShapes.toSeq: _*).build()
  }

  private def fromService[Alg[_[_, _, _, _, _]]](
      service: Service[Alg]
  ): List[Shape] = {
    val allOperationResults =
      service.endpoints.map(e => fromOperationSchema(e.schema))

    val serviceShape = addTraits(
      ServiceShape
        .builder()
        .id(service.id.toSmithy)
        .operations(allOperationResults.map(_.operationShape.getId).asJava)
        .build(),
      service.hints
    )

    allOperationResults
      .flatMap(op => op.transitiveShapes :+ op.operationShape)
      .toList :+ serviceShape
  }

  private case class OperationSchemaResult(
      operationShape: OperationShape,
      transitiveShapes: List[Shape]
  )
  private def fromOperationSchema[I, E, O, SI, SO](
      opSchema: OperationSchema[I, E, O, SI, SO]
  ): OperationSchemaResult = {
    val transitives = fromSchemas(
      List(opSchema.input, opSchema.output) ++ opSchema.error.toList
        .flatMap(_.alternatives.map(_.schema))
        .toList
    )

    val errors = opSchema.error.toList.flatMap(e =>
      e.alternatives.map(_.schema.shapeId.toSmithy)
    )

    val opShape =
      addTraits(
        OperationShape
          .builder()
          .id(opSchema.id.toSmithy)
          .addErrors(errors.asJava)
          .input(opSchema.input.shapeId.toSmithy)
          .output(opSchema.output.shapeId.toSmithy)
          .build(),
        opSchema.hints
      )

    OperationSchemaResult(opShape, transitives)
  }

  private def fromSchemas(schemas: List[Schema[_]]): List[Shape] = {
    schemas.flatMap { schema =>
      schema
        .compile(internals.conversion.ToSmithyVisitor)
        .apply(Map.empty)
        ._1
        .values
        .toList
    }
  }

  private def fromSchema[A](schema: Schema[A]): List[Shape] = {
    schema
      .compile(internals.conversion.ToSmithyVisitor)
      .apply(Map.empty)
      ._1
      .values
      .toList
  }
}
