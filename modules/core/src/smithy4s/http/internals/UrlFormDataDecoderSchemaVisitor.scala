/*
 *  Copyright 2023 Disney Streaming
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
package internals

import smithy.api.{XmlFlattened, XmlName}
// import smithy4s.http._
import smithy4s.internals.SchemaDescription
import smithy4s.schema._
import smithy4s.codecs.PayloadPath
// import smithy4s.http.UrlForm.FormData.Empty

// TODO
abstract class UrlFormDataDecoderSchemaVisitor(
    val cache: CompilationCache[UrlFormDataDecoder]
) extends SchemaVisitor.Cached[UrlFormDataDecoder] {
  compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): UrlFormDataDecoder[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    Primitive.stringParser(tag, hints) match {
      case Some(parser) => UrlFormDataDecoder.fromStringParser(desc)(parser)
      case None =>
        UrlFormDataDecoder.alwaysFailing(s"Cannot decode $desc from URL form")
    }
  }

  // TODO: Here and other functions: eat the first element from the pathed values
  // (will need to decide whether implementing a cursor is worthwhile, or whether UrlForm itself can be used).
  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): UrlFormDataDecoder[C[A]] = {
    val key = getKey(member.hints, "member")
    val isFlattened = hints.has(XmlFlattened)
    val memberReader = compile(member)
    new UrlFormDataDecoder[C[A]] {
      def decode(
          cursor: UrlFormCursor
      ): Either[UrlFormDecodeError, C[A]] = {
        val realCursor = if (isFlattened) cursor else cursor.down(key)
        realCursor match {
          case UrlFormCursor.Value(
                history,
                UrlForm.FormData.MultipleValues(values)
              ) =>
            values.zipWithIndex
              .traverse { case (elem, index) =>
                memberReader.decode(
                  UrlFormCursor
                    .Value(history.append(index), elem)
                )
              }
              .map(list => tag.fromIterator(list.iterator))
          case UrlFormCursor.Empty(_) => Right(tag.empty)
          case other =>
            Left(
              UrlFormDecodeError(
                other.history,
                s"Expected one or multiple values"
              )
            )
        }
      }
    }
  }

  override def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): UrlFormDataDecoder[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _))
    }
    compile(Schema.vector(kvSchema.addHints(XmlName("entry"))).addHints(hints))
      .map(_.toMap)
  }

  override def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): UrlFormDataDecoder[E] =
    tag match {
      case EnumTag.IntEnum =>
        val desc = s"enum[${values.map(_.intValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.intValue -> ev.value).toMap
        UrlFormDataDecoder.fromStringParser(desc)(
          _.toIntOption.flatMap(valueMap.get)
        )

      case EnumTag.StringEnum =>
        val desc = s"enum[${values.map(_.stringValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.stringValue -> ev.value).toMap
        UrlFormDataDecoder.fromStringParser(desc)(valueMap.get)
    }

  override def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): UrlFormDataDecoder[S] = {
    def fieldReader[A](field: Field[S, A]): UrlFormDataDecoder[A] = {
      val key = getKey(field.memberHints, field.label)
      compile(field.schema).down(key)

    }
    val readers = fields.map(fieldReader(_))
    new UrlFormDataDecoder[S] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, S] =
        readers.traverse(_.decode(cursor)).map(make)
    }
  }

  override def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): UrlFormDataDecoder[U] = {
    def altDecoder[A](
        alt: Alt[U, A]
    ): (PayloadPath.Segment, UrlFormDataDecoder[U]) = {
      val key = getKey(alt.hints, alt.label)
      val decoder = compile(alt.schema).map(alt.inject).down(key)
      (key, decoder)
    }
    val altMap = alternatives
      .map(altDecoder(_))
      .toMap[PayloadPath.Segment, UrlFormDataDecoder[U]]
    new UrlFormDataDecoder[U] {
      def decode(cursor: UrlFormCursor): Either[UrlFormDecodeError, U] = {
        cursor match {
          case s @ UrlFormCursor.Value(
                history,
                UrlForm.FormData.MultipleValues(
                  // TODO: Change values to list
                  Vector(pathedValue)
                )
              ) =>
            pathedValue match {
              case UrlForm.FormData
                    .PathedValue(PayloadPath(segment :: Nil), _) =>
                altMap.get(segment) match {
                  case Some(altDecoder) =>
                    altDecoder.decode(s)
                  case None =>
                    Left(
                      UrlFormDecodeError(
                        history,
                        s"Not a valid alternative: $segment"
                      )
                    )
                }
              case _ =>
                Left(UrlFormDecodeError(history, "Expected a single value"))
            }
          case other =>
            Left(UrlFormDecodeError(other.history, "Expected a single value"))
        }
      }
    }
  }

  override def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): UrlFormDataDecoder[B] =
    schema.compile(this).map(bijection.to)

  override def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): UrlFormDataDecoder[B] =
    schema.compile(this).emap(refinement.asFunction)

  override def lazily[A](suspend: Lazy[Schema[A]]): UrlFormDataDecoder[A] =
    new UrlFormDataDecoder[A] {
      lazy val underlying: UrlFormDataDecoder[A] = compile(suspend.value)
      override def decode(
          cursor: UrlFormCursor
      ): Either[UrlFormDecodeError, A] = {
        underlying.decode(cursor)
      }
    }

  def option[A](schema: Schema[A]): UrlFormDataDecoder[Option[A]] =
    compile(schema).optional

  private def getKey(hints: Hints, default: String): PayloadPath.Segment =
    hints
      .get(XmlName)
      .map(_.value)
      .map(PayloadPath.Segment(_))
      .getOrElse(default)

}
