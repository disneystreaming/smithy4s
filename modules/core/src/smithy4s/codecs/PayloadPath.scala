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

package smithy4s.codecs

import smithy4s.schema._

case class PayloadPath(segments: List[PayloadPath.Segment]) {
  def render(prefix: String = "."): String =
    segments.map(_.render).mkString(prefix, ".", "")

  override def toString = render()

  def append(segment: PayloadPath.Segment): PayloadPath =
    copy(segments.appended(segment))

  def prepend(segment: PayloadPath.Segment): PayloadPath =
    copy(segments.prepended(segment))
}

object PayloadPath {

  val root = PayloadPath(List.empty)

  def apply(segments: PayloadPath.Segment*): PayloadPath = PayloadPath(
    segments.toList
  )

  def fromString(str: String): PayloadPath = PayloadPath(
    str.split('.').filter(_.nonEmpty).map(Segment.fromString).toList
  )

  val schema: Schema[PayloadPath] =
    Schema.bijection(Schema.string, fromString, _.render())

  /**
    * A path-segment in a json-like object
    */
  sealed trait Segment {
    def render: String = this match {
      case Segment.Label(label) => label
      case Segment.Index(index) => index.toString
    }
  }

  object Segment {
    def apply(label: String): Segment = Label(label)
    def apply(index: Int): Segment = Index(index)

    // TODO: What if it was actually meant to be a string?
    def fromString(string: String): Segment = try { Index(string.toInt) }
    catch {
      case _: Throwable => Label(string)
    }

    case class Label(label: String) extends Segment
    case class Index(index: Int) extends Segment

    implicit def stringConversion(label: String): Segment = Label(label)
    implicit def intConversion(index: Int): Segment = Index(index)
  }

}
