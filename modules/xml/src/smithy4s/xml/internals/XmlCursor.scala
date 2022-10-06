package smithy4s.xml
package internals

import XmlDocument.XmlElem
import cats.data.NonEmptyList

sealed trait XmlCursor {
  def history: XPath
  def down(tag: String): XmlCursor
  def attr(name: String): XmlCursor
  def error(message: String): Either[XmlDecodeError, Nothing] = Left(
    XmlDecodeError(history, message)
  )
}

object XmlCursor {
  def fromDocument(document: XmlDocument): XmlCursor =
    Nodes(XPath.root, NonEmptyList.one(document.root))

  case class Nodes(
      history: XPath,
      nodes: NonEmptyList[XmlDocument.XmlElem]
  ) extends XmlCursor {
    def isSingle: Boolean = nodes.tail.isEmpty
    def down(tag: String): XmlCursor = if (isSingle) {
      val newHistory = history.appendTag(tag)
      val node = nodes.head
      val allNodes = node.children.collect { case e @ XmlElem(`tag`, _, _) =>
        e
      }
      NonEmptyList.fromList(allNodes) match {
        case None      => NoNode(newHistory)
        case Some(nel) => Nodes(newHistory, nel)
      }
    } else FailedNode(history.appendTag(tag))

    def attr(name: String): XmlCursor = if (isSingle) {
      val node = nodes.head
      val newHistory = history.appendAttr(name)
      val allValues = node.attributes.collect {
        case XmlDocument.XmlAttr(`name`, values) =>
          values.map(_.text)
      }.flatten
      NonEmptyList.fromList(allValues) match {
        case None      => NoNode(newHistory)
        case Some(nel) => AttrNode(newHistory, nel)
      }
    } else FailedNode(history.appendAttr(name))
  }
  case class AttrNode(history: XPath, values: NonEmptyList[String])
      extends XmlCursor {
    def down(tag: String): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: String): XmlCursor = FailedNode(history.appendAttr(name))
  }
  case class NoNode(history: XPath) extends XmlCursor {
    def down(tag: String): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: String): XmlCursor = FailedNode(history.appendAttr(name))
  }
  case class FailedNode(history: XPath) extends XmlCursor {
    def down(tag: String): XmlCursor = this
    def attr(name: String): XmlCursor = this
  }
}
