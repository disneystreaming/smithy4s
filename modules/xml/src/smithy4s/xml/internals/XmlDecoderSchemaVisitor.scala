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

package smithy4s.xml
package internals

import cats.data.NonEmptyList
import cats.syntax.all._
import smithy.api.XmlAttribute
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s._
import smithy4s.internals.SchemaDescription
import smithy4s.schema.Schema
import smithy4s.schema._

import XmlDocument.XmlQName

private[smithy4s] object XmlDecoderSchemaVisitor extends XmlDecoderSchemaVisitor

private[smithy4s] abstract class XmlDecoderSchemaVisitor
    extends SchemaVisitor[XmlDecoder]
    with smithy4s.ScalaCompat { compile =>
  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): XmlDecoder[P] = {
    val desc = SchemaDescription.primitive(shapeId, hints, tag)
    val trim = (tag != Primitive.PString && tag != Primitive.PBlob)
    Primitive.stringParser(tag, hints) match {
      case Some(parser) => XmlDecoder.fromStringParser(desc, trim)(parser)
      case None => XmlDecoder.alwaysFailing(s"Cannot decode $desc from XML")
    }
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): XmlDecoder[C[A]] = {
    val xmlName = getXmlName(member.hints, "member")
    val isFlattened = hints.has(XmlFlattened)
    val memberReader = compile(member)
    new XmlDecoder[C[A]] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, C[A]] = {
        val realCursor = if (isFlattened) cursor else cursor.down(xmlName)
        realCursor match {
          case XmlCursor.Nodes(history, nodes) =>
            nodes.zipWithIndex
              .traverse { case (elem, index) =>
                memberReader.decode(
                  XmlCursor
                    .Nodes(history.appendIndex(index), NonEmptyList.one(elem))
                )
              }
              .map(list => tag.fromIterator(list.iterator))
          case XmlCursor.NoNode(_) => Right(tag.empty)
          case other =>
            Left(
              XmlDecodeError(other.history, s"Expected one or multiple nodes")
            )
        }
      }
    }
  }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): XmlDecoder[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _))
    }
    compile(Schema.vector(kvSchema.addHints(XmlName("entry"))).addHints(hints))
      .map(_.toMap)
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag,
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): XmlDecoder[E] = {
    tag match {
      case EnumTag.IntEnum =>
        val desc = s"enum[${values.map(_.intValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.intValue -> ev.value).toMap
        XmlDecoder.fromStringParser(desc, trim = true)(
          _.toIntOption.flatMap(valueMap.get)
        )

      case EnumTag.StringEnum =>
        val desc = s"enum[${values.map(_.stringValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.stringValue -> ev.value).toMap
        XmlDecoder.fromStringParser(desc, trim = false)(valueMap.get)
    }
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): XmlDecoder[S] = {
    def fieldReader[A](field: Field[S, A]): XmlDecoder[A] = {
      val isAttribute = field.localHints.has(XmlAttribute)
      val xmlName = getXmlName(field.localHints, field.label)
      if (isAttribute) compile(field.schema).attribute(xmlName)
      else compile(field.schema).down(xmlName)

    }
    val readers = fields.map(fieldReader(_))
    new XmlDecoder[S] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, S] =
        readers.traverse(_.decode(cursor)).map(make)
    }
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[SchemaAlt[U, _]],
      dispatch: Alt.Dispatcher[Schema, U]
  ): XmlDecoder[U] = {
    def altDecoder[A](alt: SchemaAlt[U, A]): (XmlQName, XmlDecoder[U]) = {
      val xmlName = getXmlName(alt.hints, alt.label)
      val decoder = compile(alt.instance).map(alt.inject).down(xmlName)
      (xmlName, decoder)
    }
    val altMap = alternatives.map(altDecoder(_)).toMap[XmlQName, XmlDecoder[U]]
    new XmlDecoder[U] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, U] = {
        cursor match {
          case s @ XmlCursor.Nodes(history, NonEmptyList(node, Nil)) =>
            node.children match {
              case XmlDocument.XmlElem(xmlName, _, _) :: Nil =>
                altMap.get(xmlName) match {
                  case Some(altDecoder) =>
                    altDecoder.decode(s)
                  case None =>
                    Left(
                      XmlDecodeError(
                        history,
                        s"Not a valid alternative: $xmlName"
                      )
                    )
                }
              case _ =>
                Left(XmlDecodeError(history, "Expected a single node"))
            }
          case other =>
            Left(XmlDecodeError(other.history, "Expected a single node"))
        }
      }
    }
  }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): XmlDecoder[B] =
    schema.compile(this).map(bijection.to)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): XmlDecoder[B] =
    schema.compile(this).emap(refinement.asFunction)

  def lazily[A](suspend: Lazy[Schema[A]]): XmlDecoder[A] = new XmlDecoder[A] {
    lazy val underlying: XmlDecoder[A] = compile(suspend.value)
    def decode(cursor: XmlCursor): Either[XmlDecodeError, A] = {
      underlying.decode(cursor)
    }
  }

  def nullable[A](schema: Schema[A]): XmlDecoder[Option[A]] =
    compile(schema).optional

  private def getXmlName(hints: Hints, default: String): XmlDocument.XmlQName =
    hints
      .get(XmlName)
      .map(_.value)
      .map(XmlQName.parse)
      .getOrElse(XmlQName(None, default))

}
