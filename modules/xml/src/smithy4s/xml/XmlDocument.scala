package smithy4s.xml

import fs2.data.xml.dom.DocumentBuilder
import fs2.data.xml.{Attr, QName}
import fs2.data.xml.XmlEvent
import fs2.data.xml.XmlEvent.XmlCharRef
import fs2.data.xml.XmlEvent.XmlEntityRef
import fs2.data.xml.XmlEvent.XmlString

case class XmlDocument(root: XmlDocument.XmlElem)

object XmlDocument {

  // format: off
  sealed trait XmlContent
  case class XmlText(text: String)                                                       extends XmlContent
  case class XmlElem(name: QName, attributes: List[XmlAttr], children: List[XmlContent]) extends XmlContent
  case class XmlAttr(name: QName, value: List[XmlText])
  // format: on

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
          XmlAttr(attr.name, values)
        }
        Some(XmlDocument.XmlElem(name, xmlAttrs, filtered))
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

    }

}
