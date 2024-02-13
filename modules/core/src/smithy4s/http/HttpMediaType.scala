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
package http

import smithy4s.Newtype
import smithy4s.schema._

object HttpMediaType extends Newtype[String] {

  val schema: Schema[HttpMediaType] =
    Schema.bijection(Schema.string, apply, _.value)

  val id: smithy4s.ShapeId = smithy4s.ShapeId("smithy4s.http", "HttpMediaType")

  private type MaybeMediaType[A] = Option[String]
  def fromSchema[A](schema: Schema[A]): Option[Type] =
    schema.compile(MediaTypeVisitor).map(apply)
  private object MediaTypeVisitor
      extends SchemaVisitor.Default[MaybeMediaType] {
    self =>

    override def default[A]: Option[String] = None

    private def stringMediaType(hints: Hints): String =
      hints.get(smithy.api.MediaType) match {
        case Some(mediaType) => mediaType.value
        case None            => "text/plain"
      }

    override def primitive[P](
        shapeId: ShapeId,
        hints: Hints,
        tag: Primitive[P]
    ): Option[String] = tag match {
      case Primitive.PString =>
        Some(stringMediaType(hints))

      case Primitive.PBlob =>
        Some {
          hints.get(smithy.api.MediaType) match {
            case Some(mediaType) => mediaType.value
            case None            => "application/octet-stream"
          }
        }

      case _ => None
    }

    override def enumeration[E](
        shapeId: ShapeId,
        hints: Hints,
        tag: EnumTag[E],
        values: List[EnumValue[E]]
    ): Option[String] = Some(stringMediaType(hints))

    override def biject[A, B](
        schema: Schema[A],
        bijection: Bijection[A, B]
    ): Option[String] = self(schema)

    override def refine[A, B](
        schema: Schema[A],
        refinement: Refinement[A, B]
    ): Option[String] = self(schema)

    override def option[A](schema: Schema[A]): Option[String] = self(
      schema
    )
  }

}
