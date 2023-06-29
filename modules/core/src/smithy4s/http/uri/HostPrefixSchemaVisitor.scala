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

package smithy4s.http.uri

import smithy4s.http.uri.HostPrefixEncode.{MaybeHostPrefixEncode, maybeStr}
import smithy4s.{Schema, _}
import smithy4s.schema._
import HostPrefixSegment._
import smithy4s.http.internals.vectorOps

object HostPrefixSchemaVisitor
    extends SchemaVisitor[MaybeHostPrefixEncode]
    with SchemaVisitor.Default[MaybeHostPrefixEncode] {
  self =>

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
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MaybeHostPrefixEncode[E] =
    tag match {
      case EnumTag.IntEnum    => default
      case EnumTag.StringEnum => maybeStr.map(_.contramap(total(_).stringValue))
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybeHostPrefixEncode[S] = {
    type Writer = S => List[String]

    def toHostPrefixEncoder[A](
        field: Field[Schema, S, A]
    ): Option[Writer] = {
      field.fold(new Field.Folder[Schema, S, Option[Writer]] {
        def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): Option[Writer] = {
          self(instance).map(_.contramap(get).encode)
        }
        def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): Option[Writer] = None
      })
    }
    def compile1(path: HostPrefixSegment): Option[Writer] = path match {
      case Static(value) => Some(Function.const(List(value)))
      case HostLabel(value) =>
        fields
          .find(_.label == value)
          .flatMap(field => toHostPrefixEncoder(field))
    }

    def compileHostPrefix(
        hostPrefixSegments: Vector[HostPrefixSegment]
    ): Option[Vector[Writer]] =
      hostPrefixSegments.traverse(compile1(_))

    for {
      endpointHint <- hints.get[smithy.api.Endpoint]
      writers <- compileHostPrefix(
        hostPrefixSegments(endpointHint.hostPrefix.value)
      )
    } yield new HostPrefixEncode[S] {
      def encode(s: S): List[String] = writers.flatMap(_.apply(s)).toList
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

  override def lazily[A](suspend: Lazy[Schema[A]]): MaybeHostPrefixEncode[A] = {
    // "safe" because the `structure` implementation will not exercise any recursion
    // due to the fact that httpLabel can only be applied on members targeting
    // simple shapes.
    suspend.map(this.apply(_)).value
  }
}
