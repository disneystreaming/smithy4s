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
package http.internals

import smithy4s.Hints
import smithy4s.schema.Field

import smithy4s.schema.SchemaVisitor
import smithy4s.schema.SchemaField

trait FromMetadata[+A] {
  def read(metadata: Map[String, Any]): Either[String, A]
}

object FromMetadataSchemaVisitor extends SchemaVisitor.Default[FromMetadata] {
  def default[A]: FromMetadata[A] = (_: Map[String, Any]) =>
    Left("Only structs are supported")

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): FromMetadata[S] = { (metadata: Map[String, Any]) =>
    fields
      .traverse { (field: Field[Schema, S, _]) =>
        (metadata.get(field.label), field.isRequired) match {
          case (Some(value), true) =>
            Right(value)
          case (None, true) =>
            Left(s"Not found: ${field.label}")
          case (other, _) =>
            Right((other: Any))
        }
      }
      .map(make)
  }
}
