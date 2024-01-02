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

package smithy4s.http.internals

import smithy4s.{Schema, _}
import smithy4s.schema._
import smithy4s.codecs.Writer
import HostPrefixSegment._

object HostPrefixSchemaVisitor
    extends SchemaVisitor[MaybeHostPrefixEncoder]
    with SchemaVisitor.Default[MaybeHostPrefixEncoder] {
  self =>

  private val str: HostPrefixEncoder[String] =
    smithy4s.codecs.Writer.lift((_, a: String) => List(a))

  private val maybeStr: MaybeHostPrefixEncoder[String] = Some(str)

  def default[A]: MaybeHostPrefixEncoder[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MaybeHostPrefixEncoder[P] = {
    tag match {
      case Primitive.PString => maybeStr
      case _                 => default
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]]
  ): MaybeHostPrefixEncoder[E] =
    tag match {
      case EnumTag.IntEnum(_, _)        => default
      case EnumTag.StringEnum(value, _) => maybeStr.map(_.contramap(value))
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybeHostPrefixEncoder[S] = {
    def toHostPrefixEncoderr[A](
        field: Field[S, A]
    ): HostPrefixEncoder[S] =
      self(field.schema)
        .map(_.contramap(field.get))
        .getOrElse(Writer.constant(List.empty))
    def compile1(path: HostPrefixSegment): HostPrefixEncoder[S] =
      path match {
        case Static(value) => Writer.constant(List(value))
        case HostLabel(value) =>
          fields
            .find(_.label == value)
            .map(field => toHostPrefixEncoderr(field))
            .getOrElse(Writer.constant(List.empty))
      }

    def compileHostPrefix(
        hostPrefixSegments: Vector[HostPrefixSegment]
    ): Vector[HostPrefixEncoder[S]] =
      hostPrefixSegments.map(compile1(_))

    hints.get[smithy.api.Endpoint].map { endpointHint =>
      val writers = compileHostPrefix(
        hostPrefixSegments(endpointHint.hostPrefix.value)
      )
      Writer.lift { (message, s: S) =>
        writers.flatMap(_.write(message, s)).toList
      }
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): MaybeHostPrefixEncoder[B] = {
    self(schema).map(_.contramap(bijection.from))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MaybeHostPrefixEncoder[B] = {
    self(schema).map(_.contramap(refinement.from))
  }

}
