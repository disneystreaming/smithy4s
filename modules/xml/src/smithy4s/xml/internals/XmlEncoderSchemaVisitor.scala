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

package smithy4s.xml
package internals

import cats.MonoidK
import cats.syntax.all._
import smithy.api.XmlAttribute
import smithy.api.XmlFlattened
import smithy.api.XmlName
import smithy4s.schema._
import smithy4s.{Schema => _, _}

import XmlDocument._
import cats.kernel.Monoid

private[smithy4s] class XmlEncoderSchemaVisitor(
    val cache: CompilationCache[XmlEncoder]
) extends SchemaVisitor.Cached[XmlEncoder] { compile =>

  def primitive[P](
      shapeId: ShapeId,
      hints: Hints,
      tag: Primitive[P]
  ): XmlEncoder[P] = Primitive.stringWriter(tag, hints) match {
    case None => XmlEncoder.nil
    case Some(writer) =>
      new XmlEncoder[P] {
        def encode(value: P): List[XmlDocument.XmlContent] =
          List(XmlText(writer(value)))
      }
  }

  def struct[S](
      shapeId: ShapeId,
      hints: Hints,
      fields: Vector[Field[S, _]],
      make: IndexedSeq[Any] => S
  ): XmlEncoder[S] = {
    def fieldEncoder[A](field: Field[S, A]): XmlEncoder[S] = {
      val isAttribute = field.memberHints.has(XmlAttribute)
      val xmlName = getXmlName(field.memberHints, field.label)
      val namespace = getXmlNamespace(field.memberHints)
      val aEncoder =
        if (isAttribute)
          compile(field.schema).attribute(xmlName)
        else compile(field.schema).addXmlNamespace(namespace).down(xmlName)

      new XmlEncoder[S] {
        def encode(s: S): List[XmlContent] =
          field.getUnlessDefault(s) match {
            case Some(value) => aEncoder.encode(value)
            case None        => List.empty
          }
      }
    }
    implicit val monoid: Monoid[XmlEncoder[S]] = MonoidK[XmlEncoder].algebra[S]
    fields.map(fieldEncoder(_)).combineAll
  }

  def collection[C[_], A](
      shapeId: ShapeId,
      hints: Hints,
      tag: CollectionTag[C],
      member: Schema[A]
  ): XmlEncoder[C[A]] =
    new XmlEncoder[C[A]] { self =>
      val isFlattened: Boolean = hints.has(XmlFlattened)
      val xmlName = getXmlName(member.hints.memberHints, "member")
      val namespace = getXmlNamespace(member.hints.memberHints)
      val memberWriter =
        if (isFlattened) compile(member)
        else compile(member).addXmlNamespace(namespace).down(xmlName)

      def encode(value: C[A]): List[XmlContent] = {
        tag.iterator(value).toList.foldMap(memberWriter.encode)
      }

      override def down(name: XmlQName): XmlEncoder[C[A]] = {
        if (isFlattened) {
          new XmlEncoder[C[A]] {
            def encode(value: C[A]): List[XmlContent] = {
              tag.iterator(value).toList.map { a =>
                val content = memberWriter.encode(a)
                val (attributes, children) = content.partitionEither {
                  case attr @ XmlAttr(_, _) => Left(attr)
                  case other                => Right(other)
                }
                XmlElem(name, attributes, children)
              }
            }
            override def addXmlNamespace(
                maybeNs: Option[XmlAttr]
            ): XmlEncoder[C[A]] = this
          }
        } else super.down(name)
      }
    }

  def map[K, V](
      shapeId: ShapeId,
      hints: Hints,
      key: Schema[K],
      value: Schema[V]
  ): XmlEncoder[Map[K, V]] = {
    type KV = (K, V)
    val kvSchema: Schema[(K, V)] = {
      val kField = key.required[KV]("key", _._1)
      val vField = value.required[KV]("value", _._2)
      Schema.struct(kField, vField)((_, _))
    }
    compile(
      Schema.vector(kvSchema.addMemberHints(XmlName("entry"))).addHints(hints)
    )
      .contramap(_.toVector)
  }

  def enumeration[E](
      shapeId: ShapeId,
      hints: Hints,
      tag: EnumTag[E],
      values: List[EnumValue[E]]
  ): XmlEncoder[E] = tag match {
    case EnumTag.IntEnum(value, _) =>
      new XmlEncoder[E] {
        def encode(e: E): List[XmlContent] = List(
          XmlText(value(e).toString())
        )
      }

    case EnumTag.StringEnum(value, _) =>
      new XmlEncoder[E] {
        def encode(e: E): List[XmlContent] = List(
          XmlText(value(e))
        )
      }
  }

  def union[U](
      shapeId: ShapeId,
      hints: Hints,
      alternatives: Vector[Alt[U, _]],
      dispatch: Alt.Dispatcher[U]
  ): XmlEncoder[U] = new XmlEncoder[U] {
    override def encodesUnion: Boolean = true
    val underlying = dispatch.compile(new Alt.Precompiler[XmlEncoder] {
      def apply[A](label: String, instance: Schema[A]): XmlEncoder[A] = {
        val xmlName = getXmlName(instance.hints, label)
        compile(instance).down(xmlName)
      }
    })
    def encode(value: U): List[XmlContent] = underlying.encode(value)
  }

  def biject[A, B](
      schema: Schema[A],
      bijection: Bijection[A, B]
  ): XmlEncoder[B] = compile(schema).contramap(bijection.from)

  def refine[A, B](
      schema: Schema[A],
      refinement: Refinement[A, B]
  ): XmlEncoder[B] = compile(schema).contramap(refinement.from)

  def lazily[A](suspend: Lazy[Schema[A]]): XmlEncoder[A] = new XmlEncoder[A] {
    lazy val underlying = suspend.map(compile(_)).value
    def encode(value: A): List[XmlContent] = underlying.encode(value)
  }

  def option[A](schema: Schema[A]): XmlEncoder[Option[A]] =
    compile(schema).optional

  private def getXmlName(hints: Hints, default: String): XmlDocument.XmlQName =
    hints
      .get(XmlName)
      .map(_.value)
      .map(XmlQName.parse)
      .getOrElse(XmlQName(None, default))

  private def getXmlNamespace(hints: Hints): Option[XmlAttr] =
    hints
      .get(smithy.api.XmlNamespace)
      .map { ns =>
        val qName = ns.prefix match {
          case Some(prefix) => XmlQName(Some("xmlns"), prefix.value)
          case None         => XmlQName(None, "xmlns")
        }
        XmlAttr(qName, List(XmlText(ns.uri.value)))
      }

}
