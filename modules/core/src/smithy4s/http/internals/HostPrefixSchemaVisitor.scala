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

package smithy4s.http.internals

import smithy4s.{Schema, _}
import smithy4s.schema._
import smithy4s.codecs.Writer
import HostPrefixSegment._

object HostPrefixSchemaVisitor
    extends SchemaVisitor[MaybeHostPrefixEncode]
    with SchemaVisitor.Default[MaybeHostPrefixEncode] {
  self =>

  private val str: HostPrefixEncode[String] =
    smithy4s.codecs.Writer.lift((_, a: String) => List(a))

  private val maybeStr: MaybeHostPrefixEncode[String] = Some(str)

  def default[A]: MaybeHostPrefixEncode[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MaybeHostPrefixEncode[P] = {
    tag match {
      case Primitive.PString => maybeStr
      case _                 => default
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MaybeHostPrefixEncode[E] =
    tag match {
      case EnumTag.ClosedIntEnum => default
      case EnumTag.ClosedStringEnum =>
        maybeStr.map(_.contramap(total(_).stringValue))
      case _ =>
        throw new RuntimeException(
          "BOOM"
        ) // TODO: Handle open int and string enums
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybeHostPrefixEncode[S] = {
    def toHostPrefixEncoder[A](
        field: Field[S, A]
    ): HostPrefixEncode[S] =
      self(field.schema)
        .map(_.contramap(field.get))
        .getOrElse(Writer.constant(List.empty))
    def compile1(path: HostPrefixSegment): HostPrefixEncode[S] =
      path match {
        case Static(value) => Writer.constant(List(value))
        case HostLabel(value) =>
          fields
            .find(_.label == value)
            .map(field => toHostPrefixEncoder(field))
            .getOrElse(Writer.constant(List.empty))
      }

    def compileHostPrefix(
        hostPrefixSegments: Vector[HostPrefixSegment]
    ): Vector[HostPrefixEncode[S]] =
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
  ): MaybeHostPrefixEncode[B] = {
    self(schema).map(_.contramap(bijection.from))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MaybeHostPrefixEncode[B] = {
    self(schema).map(_.contramap(refinement.from))
  }

}
