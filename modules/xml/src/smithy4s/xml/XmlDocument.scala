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

import cats.syntax.all._
import fs2.Pure
import fs2.Stream
import fs2.data.xml.Attr
import fs2.data.xml.QName
import fs2.data.xml.XmlEvent
import fs2.data.xml.XmlEvent.XmlCharRef
import fs2.data.xml.XmlEvent.XmlEntityRef
import fs2.data.xml.XmlEvent.XmlString
import fs2.data.xml.dom.DocumentBuilder
import fs2.data.xml.dom.DocumentEventifier
import smithy.api.XmlName
import smithy4s.ShapeId
import smithy4s.schema.Schema
import smithy4s.xml.internals.XmlCursor
import smithy4s.xml.internals.XmlDecoderSchemaVisitor
import smithy4s.xml.internals.XmlEncoderSchemaVisitor

/**
  * A XmlDocument is an atomic piece of xml data that contains only one
  * top-level element.
  *
  * @param root
  */
final case class XmlDocument(root: XmlDocument.XmlElem)

object XmlDocument {

  /**
    * The XmlContent is the the very simple ADT that Smithy4s works against when dealing with XML.
    * It can be either a piece of text, or an XML element. Smithy4s expects XML references to be resolved
    * before this content is created.
    *
    * It is worth noting that comments and other miscellaneous elements are erased before instances of this
    * ADT are produced
    */
  // format: off
  sealed trait XmlContent extends Product with Serializable
  final case class XmlText(text: String)                                                       extends XmlContent
  final case class XmlElem(name: XmlQName, attributes: List[XmlAttr], children: List[XmlContent]) extends XmlContent
  final case class XmlAttr(name: XmlQName, values: List[XmlText]) extends XmlContent
  final case class XmlQName(prefix: Option[String], name: String) {
    override def toString : String = render
    def render: String = prefix match {
      case None => name
      case Some(p) => p + ":" + name
    }
  }
  // format: on

  object XmlQName {
    def parse(string: String): XmlQName = {
      string.lastIndexOf(':') match {
        case -1 => XmlQName(None, string)
        case index =>
          val prefix = string.slice(0, index)
          val name = string.slice(index + 1, string.length())
          XmlQName(Some(prefix), name)
      }
    }

    def fromShapeId(shapeId: ShapeId): XmlQName = {
      XmlQName(None, shapeId.name)
    }
  }

  private def getRootName[A](schema: Schema[A]): XmlQName = {
    schema.hints
      .get(XmlName)
      .map(_.value)
      .map(XmlQName.parse)
      .getOrElse(XmlQName.fromShapeId(schema.shapeId))
  }

  /**
    * A Decoder aims at decoding documents. As such, it is not meant to be a compositional construct, because
    * documents cannot be nested under other documents. This aims at decoding top-level XML payloads.
    */
  trait Decoder[A] {
    def decode(xmlDocument: XmlDocument): Either[XmlDecodeError, A]
  }

  object Decoder {
    def fromSchema[A](schema: Schema[A]): Decoder[A] = {
      val expectedRootName: XmlQName = getRootName(schema)
      val decoder = XmlDecoderSchemaVisitor(schema)
      new Decoder[A] {
        def decode(xmlDocument: XmlDocument): Either[XmlDecodeError, A] = {
          val rootName = xmlDocument.root.name
          if (rootName != expectedRootName) {
            Left(
              XmlDecodeError(
                XPath.root,
                s"Expected ${expectedRootName} XML root element, got ${rootName}"
              )
            )
          } else {
            decoder.decode(XmlCursor.fromDocument(xmlDocument))
          }
        }
      }
    }
  }

  trait Encoder[A] {
    def encode(value: A): XmlDocument
  }
  object Encoder {
    def fromSchema[A](schema: Schema[A]): Encoder[A] = {
      val rootName: XmlQName = getRootName(schema)
      val xmlEncoder = XmlEncoderSchemaVisitor(schema)
      new Encoder[A] {
        def encode(value: A): XmlDocument = {
          val (attributes, children) =
            xmlEncoder.encode(value).partitionEither {
              case attr @ XmlAttr(_, _) => Left(attr)
              case other                => Right(other)
            }
          XmlDocument(XmlElem(rootName, attributes, children))
        }
      }
    }
  }

  /**
    * This instance implements the DocumentBuilder interface provided by fs2-data, which
    * can be used to parse a stream of XML events into a stream of our XmlDocument.
    */
  implicit val documentBuilder: DocumentBuilder[XmlDocument] =
    new DocumentBuilder[XmlDocument] {

      type Content = Option[XmlContent]
      type Elem = Some[XmlDocument.XmlElem]
      type Misc = None.type

      def makeComment(content: String): Option[Misc] = None

      def makeText(texty: XmlEvent.XmlTexty): Content = texty match {
        case XmlCharRef(_)   => None
        case XmlEntityRef(_) => None
        case XmlString(s, _) =>
          if (s.trim.isEmpty) None else Some(XmlDocument.XmlText(s))
      }

      def makeElement(
          name: QName,
          attributes: List[Attr],
          isEmpty: Boolean,
          children: List[Content]
      ): Elem = {
        val filtered = children.collect { case Some(content) => content }
        val xmlAttrs = attributes.map { attr =>
          val values = attr.value.collect { case XmlString(text, _) =>
            XmlText(text)
          }
          XmlAttr(XmlQName(attr.name.prefix, attr.name.local), values)
        }
        Some(XmlDocument.XmlElem(qname(name), xmlAttrs, filtered))
      }

      def makePI(target: String, content: String): Misc = None

      def makeDocument(
          version: Option[String],
          encoding: Option[String],
          standalone: Option[Boolean],
          doctype: Option[XmlEvent.XmlDoctype],
          prolog: List[Misc],
          root: Elem,
          postlog: List[Misc]
      ): XmlDocument = XmlDocument(root.get)

      private def qname(name: QName): XmlQName =
        XmlQName(name.prefix, name.local)

    }

  implicit val documentEventifier: DocumentEventifier[XmlDocument] =
    new DocumentEventifier[XmlDocument] {
      def eventify(node: XmlDocument): Stream[Pure, XmlEvent] = {
        Stream(XmlEvent.StartDocument) ++
          eventifyContent(node.root) ++
          Stream(XmlEvent.EndDocument)
      }
      def eventifyContent(xmlContent: XmlContent): Stream[Pure, XmlEvent] =
        xmlContent match {
          case XmlText(text) =>
            Stream(XmlEvent.XmlString(text, isCDATA = false))
          case XmlElem(name, attributes, children) =>
            val qName = toQName(name)
            val attr: List[Attr] = attributes.map(toAttr)
            if (children.isEmpty) {
              Stream(XmlEvent.StartTag(qName, attr, isEmpty = true))
            } else {
              Stream(XmlEvent.StartTag(qName, attr, isEmpty = false)) ++
                children.foldMap(eventifyContent) ++
                Stream(XmlEvent.EndTag(qName))
            }
          case XmlAttr(_, _) => Stream.empty
        }

      private def toAttr(attr: XmlAttr): Attr = Attr(
        toQName(attr.name),
        attr.values.map(text => XmlEvent.XmlString(text.text, isCDATA = false))
      )

      private def toQName(name: XmlQName): QName = QName(name.prefix, name.name)
    }

}
