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
package http

import smithy4s.PartialData
import smithy.api.HttpPayload
import smithy4s.schema._
import smithy4s.schema.SchemaPartition.NoMatch
import smithy4s.schema.SchemaPartition.SplittingMatch
import smithy4s.schema.SchemaPartition.TotalMatch

/**
 * This construct indicates how a schema is split between http metadata
 * (ie headers, path parameters, query parameters, status code) and body.
 *
 * The schema is split between two different
 */
sealed trait HttpRestSchema[A]

object HttpRestSchema {

  // format: off
  final case class OnlyMetadata[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class OnlyBody[A](schema: Schema[A]) extends HttpRestSchema[A]
  final case class MetadataAndBody[A](metadataSchema: Schema[PartialData[A]], bodySchema: Schema[PartialData[A]]) extends HttpRestSchema[A]
  final case class Empty[A](value: A) extends HttpRestSchema[A]
  // format: on

  def apply[A](fullSchema: Schema[A]): HttpRestSchema[A] = {

    def isMetadataField(field: SchemaField[_, _]): Boolean = HttpBinding
      .fromHints(field.label, field.instance.hints, fullSchema.hints)
      .isDefined

    def isPayloadField(field: SchemaField[_, _]): Boolean =
      field.instance.hints.has[HttpPayload]

    fullSchema.findPayload(isPayloadField) match {
      case TotalMatch(schema) => OnlyBody(schema)
      case NoMatch() =>
        fullSchema.partition(isMetadataField) match {
          case SplittingMatch(metadataSchema, bodySchema) =>
            MetadataAndBody(metadataSchema, bodySchema)
          case TotalMatch(schema) =>
            OnlyMetadata(schema)
          case NoMatch() =>
            fullSchema match {
              case Schema.PrimitiveSchema(_, _, Primitive.PUnit) => Empty(())
              case _ => OnlyBody(fullSchema)
            }
        }
      case SplittingMatch(bodySchema, metadataSchema) =>
        MetadataAndBody(metadataSchema, bodySchema)
    }
  }

}
