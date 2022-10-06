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
    SingleNode(XPath.root, document.root)

  case class SingleNode(history: XPath, node: XmlDocument.XmlElem)
      extends XmlCursor {
    def down(tag: String): XmlCursor = {
      val newHistory = history.appendTag(tag)
      val allNodes = node.children.collect { case e @ XmlElem(`tag`, _, _) =>
        e
      }
      if (allNodes.size == 1) SingleNode(newHistory, allNodes.head)
      else
        NonEmptyList.fromList(allNodes) match {
          case None      => NoNode(newHistory)
          case Some(nel) => MultipleNodes(newHistory, nel)
        }
    }
    def attr(name: String): XmlCursor = {
      val newHistory = history.appendAttr(name)
      val allValues = node.attributes.collect {
        case XmlDocument.XmlAttr(`name`, values) =>
          values.map(_.text)
      }.flatten
      NonEmptyList.fromList(allValues) match {
        case None      => NoNode(newHistory)
        case Some(nel) => AttrNode(newHistory, nel)
      }
    }
  }
  case class MultipleNodes(
      history: XPath,
      nodes: NonEmptyList[XmlDocument.XmlElem]
  ) extends XmlCursor {
    def down(tag: String): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: String): XmlCursor = FailedNode(history.appendAttr(name))
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
