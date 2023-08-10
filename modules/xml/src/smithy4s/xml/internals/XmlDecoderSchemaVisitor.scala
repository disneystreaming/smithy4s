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

import cats.syntax.all._
import smithy.api.XmlAttribute
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s._
import smithy4s.internals.SchemaDescription
import smithy4s.schema.Schema
import smithy4s.schema._

import XmlDocument.XmlQName

private[smithy4s] class XmlDecoderSchemaVisitor(
    val cache: CompilationCache[XmlDecoder]
) extends SchemaVisitor.Cached[XmlDecoder]
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
          case XmlCursor.SingleNode(history, node) =>
            memberReader
              .decode(
                XmlCursor.SingleNode(history.appendIndex(0), node)
              )
              .map(value => tag.fromIterator(Iterator.single(value)))
          case XmlCursor.Nodes(history, nodes) =>
            nodes.zipWithIndex
              .traverse { case (elem, index) =>
                memberReader.decode(
                  XmlCursor.SingleNode(history.appendIndex(index), elem)
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
      tag: EnumTag[E],
      values: List[EnumValue[E]],
      total: E => EnumValue[E]
  ): XmlDecoder[E] = {
    tag match {
      case EnumTag.IntEnum() =>
        val desc = s"enum[${values.map(_.intValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.intValue -> ev.value).toMap
        val handler: String => Option[E] =
          tag match {
            case EnumTag.OpenIntEnum(unknown) =>
              _.toIntOption.map(i => valueMap.getOrElse(i, unknown(i)))
            case _ =>
              _.toIntOption.flatMap(valueMap.get)
          }
        XmlDecoder.fromStringParser(desc, trim = true)(
          handler(_)
        )
      case _ =>
        val desc = s"enum[${values.map(_.stringValue).mkString(", ")}]"
        val valueMap = values.map(ev => ev.stringValue -> ev.value).toMap
        val handler: String => Option[E] =
          tag match {
            case EnumTag.OpenStringEnum(unknown) =>
              s => Some(valueMap.getOrElse(s, unknown(s)))
            case _ =>
              valueMap.get(_)
          }
        XmlDecoder.fromStringParser(desc, trim = false)(handler)
    }
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): XmlDecoder[S] = {
    def fieldReader[A](field: Field[S, A]): XmlDecoder[A] = {
      val decoderWithAnyDefaultValue = field.getDefaultValue match {
        case None => compile(field.schema)
        case Some(defaultValue) =>
          compile(field.schema).withDefault(defaultValue)
      }
      val isAttribute = field.memberHints.has(XmlAttribute)
      val xmlName = getXmlName(field.memberHints, field.label)
      if (isAttribute) decoderWithAnyDefaultValue.attribute(xmlName)
      else decoderWithAnyDefaultValue.down(xmlName)
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
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): XmlDecoder[U] = {
    def altDecoder[A](alt: Alt[U, A]): (XmlQName, XmlDecoder[U]) = {
      val xmlName = getXmlName(alt.hints, alt.label)
      val decoder = compile(alt.schema).map(alt.inject).down(xmlName)
      (xmlName, decoder)
    }
    val altMap = alternatives.map(altDecoder(_)).toMap[XmlQName, XmlDecoder[U]]
    new XmlDecoder[U] {
      def decode(cursor: XmlCursor): Either[XmlDecodeError, U] = {
        cursor match {
          case s @ XmlCursor.SingleNode(history, node) =>
            val children = node.children.flatMap {
              case text @ XmlDocument.XmlText(value) =>
                // Remove newlines or other blank text nodes at this level
                if (value.exists(c => !c.isWhitespace)) Some(text) else None
              case other => Some(other)
            }
            children match {
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
              case other =>
                Left(
                  XmlDecodeError(
                    history,
                    s"Expected a single node but found $other"
                  )
                )
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

  def option[A](schema: Schema[A]): XmlDecoder[Option[A]] =
    compile(schema).optional

  private def getXmlName(
      hints: Hints,
      default: String
  ): XmlDocument.XmlQName =
    hints
      .get(XmlName)
      .map(_.value)
      .map(XmlQName.parse)
      .getOrElse(XmlQName(None, default))

}
