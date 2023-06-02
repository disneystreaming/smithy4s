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

package smithy4s.internals

import smithy4s.schema._
import smithy4s.http.internals.PathEncode
import smithy4s.http.internals.PathEncode.MaybePathEncode
import smithy4s.Bijection
import smithy4s.{Hints, Lazy, Refinement, ShapeId}

private[internals] final class SchemaVisitorPatternEncoder(
    segments: List[PatternSegment]
) extends SchemaVisitor[MaybePathEncode]
    with SchemaVisitor.DefaultIgnoringInput[MaybePathEncode] {
  self =>

  def default[A]: MaybePathEncode[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MaybePathEncode[P] = {
    Primitive.stringWriter(tag, hints) match {
      case Some(writer) => PathEncode.from(e => writer(e))
      case None         => None
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): MaybePathEncode[E] =
    tag match {
      case EnumTag.IntEnum =>
        PathEncode.from(e => total(e).intValue.toString)
      case EnumTag.StringEnum =>
        PathEncode.from(e => total(e).stringValue)
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[SchemaField[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybePathEncode[S] = {
    type Writer = S => List[String]

    def toPathEncoder[A](
        field: Field[Schema, S, A]
    ): Option[Writer] = {
      field.fold(new Field.Folder[Schema, S, Option[Writer]] {
        def onRequired[AA](
            label: String,
            instance: Schema[AA],
            get: S => AA
        ): Option[Writer] =
          self(instance).map(_.contramap(get).encode)

        def onOptional[AA](
            label: String,
            instance: Schema[AA],
            get: S => Option[AA]
        ): Option[Writer] = None
      })
    }
    def compile1(path: PatternSegment): Option[Writer] = path match {
      case PatternSegment.StaticSegment(value) =>
        Some(Function.const(List(value)))
      case PatternSegment.ParameterSegment(value, _) =>
        fields
          .find(_.label == value)
          .flatMap(field => toPathEncoder(field))
    }

    def compilePath(path: Vector[PatternSegment]): Option[Vector[Writer]] =
      path.traverse(compile1(_))
    for {
      writers <- compilePath(segments.toVector)
    } yield new PathEncode[S] {
      def encode(s: S): List[String] = writers.flatMap(_.apply(s)).toList
      def encodeGreedy(s: S): List[String] = Nil
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): MaybePathEncode[B] = {
    self(schema).map(_.contramap(bijection.from))
  }

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): MaybePathEncode[B] = {
    self(schema).map(_.contramap(refinement.from))
  }

  override def lazily[A](
      shapeId: ShapeId,
      hints: Hints,
      suspend: Lazy[Schema[A]]
  ): MaybePathEncode[A] = None

}
