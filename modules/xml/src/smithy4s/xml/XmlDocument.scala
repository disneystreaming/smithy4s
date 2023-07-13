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
import fs2.data.xml.XmlEvent.XmlString
import fs2.data.xml.dom.DocumentBuilder
import fs2.data.xml.dom.DocumentEventifier
import smithy.api.XmlName
import smithy4s.ShapeId
import smithy4s.schema.Schema
import smithy4s.xml.internals.XmlCursor
import smithy4s.xml.internals.XmlDecoderSchemaVisitor
import smithy4s.xml.internals.XmlEncoderSchemaVisitor
import smithy4s.schema.CachedSchemaCompiler

/**
  * A XmlDocument is an atomic piece of xml data that contains only one
  * top-level element.
  *
  * @param root
  */
final case class XmlDocument(root: XmlDocument.XmlElem)

// scalafmt: {maxColumn = 120}
object XmlDocument {

  /**
    * The XmlContent is the the very simple ADT that Smithy4s works against when dealing with XML.
    * It can be either a piece of text, or an XML element. Smithy4s expects XML references to be resolved
    * before this content is created.
    *
    * It is worth noting that comments and other miscellaneous elements are erased before instances of this
    * ADT are produced
    */
  sealed trait XmlContent extends Product with Serializable
  final case class XmlText(text: String) extends XmlContent
  final case class XmlEntityRef(entityName: String) extends XmlContent {
    def text: String =
      entityName match {
        case "lt"   => "<"
        case "gt"   => ">"
        case "amp"  => "&"
        case "apos" => "'"
        case "quot" => "\""
        case other  => buildString(other)
      }

    private def buildString(name: String) = s"&$name;"

  }
  final case class XmlElem(name: XmlQName, attributes: List[XmlAttr], children: List[XmlContent]) extends XmlContent
  final case class XmlAttr(name: XmlQName, values: List[XmlText]) extends XmlContent
  final case class XmlQName(prefix: Option[String], name: String) {
    override def toString: String = render
    def render: String = prefix match {
      case None    => name
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

  private def getStartingPath[A](schema: Schema[A]): List[XmlQName] = {
    schema.hints
      .get(internals.XmlStartingPath)
      .map(_.path.map(XmlQName.parse))
      .getOrElse(List(getRootName(schema)))
  }

  /**
    * A Decoder aims at decoding documents. As such, it is not meant to be a compositional construct, because
    * documents cannot be nested under other documents. This aims at decoding top-level XML payloads.
    */
  trait Decoder[A] {
    def decode(xmlDocument: XmlDocument): Either[XmlDecodeError, A]
  }

  object Decoder extends CachedSchemaCompiler.Impl[Decoder] {
    def fromSchema[A](schema: Schema[A], cache: Cache): Decoder[A] = {
      val startingPath: List[XmlQName] = getStartingPath(schema)
      new Decoder[A] {
        val decoder = XmlDecoderSchemaVisitor(schema)
        def decode(xmlDocument: XmlDocument): Either[XmlDecodeError, A] = {
          val documentCursor = XmlCursor.fromDocument(xmlDocument)
          val updatedCursor = startingPath.foldLeft(documentCursor)(_.down(_))
          decoder.decode(updatedCursor)
        }
      }
    }
  }

  trait Encoder[A] {
    def encode(value: A): XmlDocument
  }
  object Encoder extends CachedSchemaCompiler.Impl[Encoder] {
    def fromSchema[A](schema: Schema[A], cache: Cache): Encoder[A] = {
      val rootName: XmlQName = getRootName(schema)
      val rootNamespace =
        schema.hints
          .get(smithy.api.XmlNamespace)
          .toList
          .map { ns =>
            val qName = ns.prefix match {
              case Some(prefix) => XmlQName(Some("xmlns"), prefix.value)
              case None         => XmlQName(None, "xmlns")
            }
            XmlAttr(qName, List(XmlText(ns.uri.value)))
          }
      val xmlEncoder = XmlEncoderSchemaVisitor(schema)
      new Encoder[A] {
        def encode(value: A): XmlDocument = {
          val (attributes, children) =
            xmlEncoder.encode(value).partitionEither {
              case attr @ XmlAttr(_, _) => Left(attr)
              case other                => Right(other)
            }
          XmlDocument(XmlElem(rootName, rootNamespace ++ attributes, children))
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

      def makeText(texty: XmlEvent.XmlTexty): Content = {
        texty match {
          case XmlCharRef(_) => None
          case XmlEvent.XmlEntityRef(name) =>
            Some(XmlDocument.XmlEntityRef(name))
          case XmlString(s, _) => Some(XmlDocument.XmlText(s))
        }
      }

      def makeElement(
          name: QName,
          attributes: List[Attr],
          isEmpty: Boolean,
          children: List[Content]
      ): Elem = {
        val filtered = children.collect { case Some(content) => content }
        val hasElems = filtered.exists {
          case _: XmlElem => true
          case _          => false
        }
        // if the children have some xml elements, filtering whitespace around them.
        val filtered2 = if (hasElems) filtered.filter {
          case XmlText(text) if text.forall(_.isWhitespace) => false
          case _                                            => true
        }
        else filtered

        val xmlAttrs = attributes.map { attr =>
          val values = attr.value.collect { case XmlString(text, _) =>
            XmlText(text)
          }
          XmlAttr(XmlQName(attr.name.prefix, attr.name.local), values)
        }
        Some(XmlDocument.XmlElem(qname(name), xmlAttrs, filtered2))
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
          case XmlDocument.XmlEntityRef(entityName) =>
            Stream.emit(XmlEvent.XmlEntityRef(entityName))
          case XmlElem(name, attributes, children) =>
            val qName = toQName(name)
            val attr: List[Attr] = attributes.map(toAttr)
            if (children.isEmpty) {
              Stream(XmlEvent.StartTag(qName, attr, isEmpty = true), XmlEvent.EndTag(qName))
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
