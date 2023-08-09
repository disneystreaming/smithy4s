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
package internals

import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s.codecs.PayloadPath
import smithy4s.internals.SchemaDescription
import smithy4s.schema._

private[http] class UrlFormDataDecoderSchemaVisitor(
    val cache: CompilationCache[UrlFormDataDecoder],
    // These are used by AwsEc2QueryCodecs to conform to the requirements of
    // https://smithy.io/2.0/aws/protocols/aws-ec2-query-protocol.html?highlight=ec2%20query%20protocol#query-key-resolution.
    ignoreXmlFlattened: Boolean,
    capitalizeStructAndUnionMemberNames: Boolean
) extends SchemaVisitor.Cached[UrlFormDataDecoder]
    with smithy4s.ScalaCompat {
  compile =>

  override def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): UrlFormDataDecoder[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    Primitive.stringParser(tag, hints) match {
      case Some(parser) =>
        UrlFormDataDecoder.fromStringParser(desc)(parser)

      case None =>
        UrlFormDataDecoder.alwaysFailing(s"Cannot decode $desc from URL form")
    }
  }

  override def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): UrlFormDataDecoder[C[A]] = {
    val memberDecoder = compile(member)
    val maybeKey =
      if (ignoreXmlFlattened || hints.has[XmlFlattened]) None
      else Option(getKey(member.hints, "member"))
    cursor =>
      maybeKey.fold(cursor)(cursor.down(_)) match {
        case UrlFormCursor(_, Nil) =>
          Right(tag.empty)

        case UrlFormCursor(history, values) =>
          // Collection members aren't necessarily primitives, and if they
          // aren't, then there will be multiple values for the same
          // index. One example is maps, which are encoded as collections of
          // structs, e.g.
          // foos.entry.1.key=a&foos.entry.1.value=1&foos.entry.2.key=b&foos.entry.2.value=2.
          // That's why we have to group by index.
          //
          // We can't assume they were encoded in order. That's why we have to
          // then sort by index.
          val groupedAndSortedCursors = values
            .collect {
              case formData @ UrlForm.FormData(
                    PayloadPath(PayloadPath.Segment.Index(index) :: _),
                    _
                  ) =>
                index -> formData
            }
            .groupBy { case (index, _) =>
              index
            }
            .toVector
            .sortBy { case (index, _) =>
              index
            }
            .map { case (index, indicesAndValues) =>
              UrlFormCursor(
                history,
                indicesAndValues.map { case (_, value) => value }
              )
                .down(PayloadPath.Segment.Index(index))
            }
          groupedAndSortedCursors
            .traverse[UrlFormDecodeError, A](memberDecoder.decode(_))
            .map(list => tag.fromIterator(list.iterator))
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
      Schema.struct(kField, vField)((_, _)).addHints(XmlName("entry"))
    }
    compile(Schema.vector(kvSchema).addHints(hints))
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
          _.toIntOption.flatMap(valueMap.get(_))
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
    def fieldDecoder[A](field: Field[S, A]): UrlFormDataDecoder[A] =
      compile(field.schema).down(getKey(field.hints, field.label))
    val decoders = fields.map(fieldDecoder(_))
    cursor =>
      decoders
        .traverse((decoder: UrlFormDataDecoder[_]) => decoder.decode(cursor))
        .map(make)
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
    ({
      case cursor @ UrlFormCursor(
            history,
            UrlForm.FormData(PayloadPath(segment :: Nil), _) :: Nil
          ) =>
        altMap.get(segment) match {
          case Some(altDecoder) =>
            altDecoder.decode(cursor)

          case None =>
            Left(
              UrlFormDecodeError(
                history,
                s"Not a valid alternative: $segment"
              )
            )
        }

      case cursor =>
        Left(UrlFormDecodeError.singleValueExpected(cursor))
    })
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

  override def lazily[A](suspend: Lazy[Schema[A]]): UrlFormDataDecoder[A] = {
    lazy val underlying: UrlFormDataDecoder[A] = compile(suspend.value)
    underlying.decode(_)
  }

  override def option[A](schema: Schema[A]): UrlFormDataDecoder[Option[A]] =
    compile(schema).optional

  private def getKey(hints: Hints, default: String): PayloadPath.Segment =
    hints
      .get(XmlName)
      .map(_.value)
      .map(PayloadPath.Segment(_))
      .getOrElse(
        if (capitalizeStructAndUnionMemberNames) default.capitalize
        else default
      )

}
