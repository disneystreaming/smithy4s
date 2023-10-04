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
import smithy4s.xml.XPath.Segment.Attr
import smithy4s.xml.XPath.Segment.Index
import smithy4s.xml.XPath.Segment.Tag
import smithy4s.xml.XmlDocument.XmlQName
import smithy4s.codecs.PayloadPath

/**
  * Represents a path in the XML payload. Segments can be either tags, indexes (when dealing with collections), or attributes.
  *
  * This allows, in particular, to identify which part of the payload is faulty, during decoding.
  */
case class XPath(reversedSegments: List[XPath.Segment]) {
  def render: String = reversedSegments.reverse.map(_.render).mkString(".")
  def toPayloadPath: PayloadPath = PayloadPath {
    reversedSegments.reverse.map { xpathSegment =>
      xpathSegment match {
        case Index(index) => PayloadPath.Segment(index)
        case Tag(tag)     => PayloadPath.Segment(tag.name)
        case Attr(attr)   => PayloadPath.Segment("attr:" + attr)
      }
    }
  }

  def appendIndex(index: Int): XPath = XPath(
    XPath.Segment.Index(index) :: reversedSegments
  )
  def appendTag(tag: XmlQName): XPath = XPath(
    XPath.Segment.Tag(tag) :: reversedSegments
  )
  def appendTag(tag: String): XPath = XPath(
    XPath.Segment.Tag(XmlQName.parse(tag)) :: reversedSegments
  )
  def appendAttr(name: XmlQName): XPath = XPath(
    XPath.Segment.Attr(name) :: reversedSegments
  )
}

object XPath {
  val root = XPath(List.empty)

  def attr(name: XmlQName): XPath = XPath(List(XPath.Segment.Attr(name)))

  sealed trait Segment {
    def render: String = this match {
      case Index(index) => s"[index:$index]"
      case Tag(tag)     => tag.render
      case Attr(attr)   => s"attr:${attr.render}"
    }
  }
  object Segment {
    final case class Index(index: Int) extends Segment
    final case class Tag(tag: XmlQName) extends Segment
    final case class Attr(attr: XmlQName) extends Segment
  }

}
