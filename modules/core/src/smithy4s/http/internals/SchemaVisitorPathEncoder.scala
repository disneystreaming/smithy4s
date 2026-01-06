/*
 *  Copyright 2021-2026 Disney Streaming
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

import smithy.api.Http
import smithy.api.TimestampFormat
import smithy4s.Bijection
import smithy4s.Hints
import smithy4s.Lazy
import smithy4s.Refinement
import smithy4s.ShapeId
import smithy4s.http.PathSegment
import smithy4s.http.PathSegment.GreedySegment
import smithy4s.http.PathSegment.LabelSegment
import smithy4s.http.PathSegment.StaticSegment
import smithy4s.schema._

import PathEncode.MaybePathEncode

class SchemaVisitorPathEncoder(urlEncodeHttpLabelValues: Boolean)
    extends SchemaVisitor[MaybePathEncode]
    with SchemaVisitor.Default[MaybePathEncode] {
  self =>

  def default[A]: MaybePathEncode[A] = None

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): MaybePathEncode[P] = {
    tag match {
      case Primitive.PShort =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PInt =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PFloat =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PLong =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PDouble =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PBigInt =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PBigDecimal =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PBoolean =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PString =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PUUID =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PByte =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PBlob     => default
      case Primitive.PDocument => default
      case Primitive.PTimestamp =>
        val fmt =
          hints.get(TimestampFormat).getOrElse(TimestampFormat.DATE_TIME)
        Some(
          PathEncode.raw(
            _.format(fmt),
            urlEncode = this.urlEncodeHttpLabelValues
          )
        )
      case Primitive.PLocalDate =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PLocalTime =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.PDuration =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
      case Primitive.POffsetDateTime =>
        PathEncode.fromToString(urlEncode = this.urlEncodeHttpLabelValues)
    }
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]]
  ): MaybePathEncode[E] =
    tag match {
      case EnumTag.IntEnum(value, _) =>
        PathEncode.from(
          e => value(e).toString,
          urlEncode = this.urlEncodeHttpLabelValues
        )
      case EnumTag.StringEnum(value, _) =>
        PathEncode.from(
          value,
          urlEncode = this.urlEncodeHttpLabelValues
        )
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): MaybePathEncode[S] = {
    type Writer = S => List[String]

    def toPathEncoder[A](
        field: Field[S, A],
        greedy: Boolean
    ): Option[Writer] = {
      val writer =
        self(field.schema).map(_.contramap(field.get))
      if (greedy) writer.map(_.encodeGreedy)
      else writer.map(_.encode)
    }

    def compile1(path: PathSegment): Option[Writer] = path match {
      case StaticSegment(value) => Some(Function.const(List(value)))
      case LabelSegment(value) =>
        fields
          .find(_.label == value)
          .flatMap(field => toPathEncoder(field, greedy = false))
      case GreedySegment(value) =>
        fields
          .find(_.label == value)
          .flatMap(field => toPathEncoder(field, greedy = true))
    }

    def compilePath(path: Vector[PathSegment]): Option[Vector[Writer]] =
      path.traverse(compile1(_))

    for {
      httpHint <- hints.get[Http]
      path <- pathSegments(httpHint.uri.value)
      writers <- compilePath(path)
    } yield new PathEncode[S] {
      override def encode(s: S): List[String] =
        writers.flatMap(_.apply(s)).toList
      override def encodeGreedy(s: S): List[String] = Nil
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

  override def lazily[A](suspend: Lazy[Schema[A]]): MaybePathEncode[A] = {
    // "safe" because the `structure` implementation will not exercise any recursion
    // due to the fact that httpLabel can only be applied on members targeting
    // simple shapes.
    suspend.map(this.apply(_)).value
  }
}
