/*
 *  Copyright 2021-2023 Disney Streaming
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

import XmlDocument.XmlElem
import XmlDocument.XmlQName

/**
  * This construct is an internal implementation-detail used for decoding XML payloads.
  *
  * It echoes the model popularised by Argonaut and Circe, where "cursors" are used instead
  * of the direct data. This makes it easier to express the decoding logic that needs
  * to "peek further down" the XML data.
  */

private[smithy4s] sealed trait XmlCursor {
  def history: XPath
  def down(tag: XmlQName): XmlCursor
  def attr(name: XmlQName): XmlCursor
}

private[smithy4s] object XmlCursor {
  def fromDocument(document: XmlDocument): XmlCursor =
    RootNode(document.root)

  case class RootNode(root: XmlDocument.XmlElem) extends XmlCursor {
    val history = XPath.root
    def down(tag: XmlQName): XmlCursor = {
      val newHistory = history.appendTag(tag)
      if (root.name != tag) FailedNode(history.appendTag(tag))
      else SingleNode(newHistory, root)
    }
    def attr(name: XmlQName): XmlCursor = FailedNode(history.appendAttr(name))

  }

  case class SingleNode(history: XPath, node: XmlDocument.XmlElem)
      extends XmlCursor {
    def down(tag: XmlQName): XmlCursor = {
      val newHistory = history.appendTag(tag)
      val matchingNodes = node.children.collect {
        case e @ XmlElem(`tag`, _, _) =>
          e
      }
      matchingNodes match {
        case Nil          => NoNode(newHistory)
        case head :: Nil  => SingleNode(newHistory, head)
        case head :: tail => Nodes(newHistory, NonEmptyList(head, tail))
      }
    }
    def attr(name: XmlQName): XmlCursor = {
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

  case class Nodes(
      history: XPath,
      nodes: NonEmptyList[XmlDocument.XmlElem]
  ) extends XmlCursor {
    def down(tag: XmlQName): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: XmlQName): XmlCursor = FailedNode(history.appendAttr(name))
  }
  case class AttrNode(history: XPath, values: NonEmptyList[String])
      extends XmlCursor {
    def down(tag: XmlQName): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: XmlQName): XmlCursor = FailedNode(history.appendAttr(name))
  }
  case class NoNode(history: XPath) extends XmlCursor {
    def down(tag: XmlQName): XmlCursor = FailedNode(history.appendTag(tag))
    def attr(name: XmlQName): XmlCursor = FailedNode(history.appendAttr(name))
  }
  case class FailedNode(history: XPath) extends XmlCursor {
    def down(tag: XmlQName): XmlCursor = this
    def attr(name: XmlQName): XmlCursor = this
  }
}
