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
package http.internals

import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Refinement
import smithy4s.ShapeId
import smithy4s.schema.Primitive.PString
import smithy4s.schema._

/**
  * A schema visitor that allows to merge several values into a single, comma-separated header value.
  * The logic for quoting is meant to abide by AWS' convoluted standards.
  *
  * See https://github.com/awslabs/smithy/pull/1798
  */
object SchemaVisitorHeaderMerge
    extends SchemaVisitor.Default[AwsMergeableHeader] {
  self =>

  def default[A]: AwsMergeableHeader[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): AwsMergeableHeader[P] = tag match {
    case PString =>
      Some { (str: String) =>
        if (str.contains('"') || str.contains(","))
          "\"" + str.replace("\"", "\\\"") + "\""
        else str
      }
    case _ => Primitive.stringWriter(tag, hints)
  }
  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): AwsMergeableHeader[B] =
    schema.compile(self).map(_.compose(bijection.from(_)))

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): AwsMergeableHeader[B] =
    schema.compile(self).map(_.compose(refinement.from(_)))

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): AwsMergeableHeader[E] = tag match {
    case EnumTag.IntEnum() => Some((e: E) => total(e).intValue.toString())
    case _                 => Some((e: E) => total(e).stringValue)
  }

}
